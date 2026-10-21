package com.joelj.mcasocial.social;

import com.joelj.mcasocial.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class SpontaneousMessageService {
    private static final long CHECK_INTERVAL = 2400L;
    private static final int DAILY_LIMIT = 2;
    private static final long CONTACT_COOLDOWN_DAYS = 3L;

    private SpontaneousMessageService() {}

    public static void tick(ServerPlayer player) {
        ISocialContacts contacts = player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).orElse(null);
        if (contacts == null) return;

        if (hasAnyDevice(player)) {
            int deliveryInterface = availableInterface(player, contacts.getPreferredInterfaceId());
            for (SpontaneousSocialMessage message : contacts.deliverSpontaneousReplies(gameTime(player))) {
                SocialContact contact = contacts.getContact(message.contactUuid());
                if (contact == null || DeceasedVillagers.get(player.server).contains(message.contactUuid())) continue;
                player.sendSystemMessage(Component.translatable("chat.mcasocial.spontaneous.notification", contact.getName())
                        .append(Component.literal(" ")).append(Component.translatable(message.responseKey())));
                SocialInterfaceSounds.playDelivered(player, deliveryInterface);
            }
            if (contacts.isSpontaneousEnabled()) {
                for (SpontaneousSocialMessage message : contacts.deliverSpontaneousMessages(gameTime(player),
                        pending -> deliveryDialogueKey(player, contacts.getContact(pending.contactUuid()), pending.dialogueKey()))) {
                    SocialContact contact = contacts.getContact(message.contactUuid());
                    if (contact == null || DeceasedVillagers.get(player.server).contains(message.contactUuid())) continue;
                    Component dialogue = Component.translatable(message.dialogueKey());
                    String greetingKey = greetingKey(player, contact, message.id());
                    if (!greetingKey.isEmpty()) {
                        dialogue = Component.translatable(greetingKey).append(Component.literal(" ")).append(dialogue);
                    }
                    player.sendSystemMessage(Component.translatable("chat.mcasocial.spontaneous.notification", contact.getName())
                            .append(Component.literal(" ")).append(dialogue));
                    SocialInterfaceSounds.playDelivered(player, deliveryInterface);
                }
            }
        }
        if (!contacts.isSpontaneousEnabled()) return;

        long now = gameTime(player);
        if (contacts.getNextSpontaneousCheck() == 0) {
            contacts.setNextSpontaneousCheck(now + CHECK_INTERVAL);
            return;
        }
        if (now < contacts.getNextSpontaneousCheck()) return;
        contacts.setNextSpontaneousCheck(now + CHECK_INTERVAL);
        if (!hasAnyDevice(player)) return;

        long day = Math.floorDiv(player.server.overworld().getDayTime(), 24000L);
        if (contacts.getSpontaneousCount(day) >= DAILY_LIMIT) return;

        List<SocialContact> eligible = new ArrayList<>();
        int totalWeight = 0;
        for (SocialContact contact : contacts.getContacts()) {
            if (!eligible(contact, day, player)) continue;
            eligible.add(contact);
            totalWeight += weight(contact);
        }
        if (totalWeight == 0) return;

        int choice = ThreadLocalRandom.current().nextInt(totalWeight);
        SocialContact selected = null;
        for (SocialContact contact : eligible) {
            choice -= weight(contact);
            if (choice < 0) {
                selected = contact;
                break;
            }
        }
        if (selected == null || ThreadLocalRandom.current().nextDouble() >= chance(selected)) return;

        int interfaceType = availableInterface(player, contacts.getPreferredInterfaceId());
        int variant = variant(selected);
        String key = SocialKinship.spontaneousKey(player, selected, variant,
                "chat.mcasocial.spontaneous." + role(selected) + "." + variant);
        long deliveryTime = now + contacts.getDeliveryDelayMode().delayTicks(interfaceType);
        contacts.addSpontaneousMessage(new SpontaneousSocialMessage(
                UUID.randomUUID(), selected.getUuid(), key, interfaceType, deliveryTime,
                false, false, "", 0L, true));
        selected.recordSpontaneousMessage(day, variant);
        contacts.recordSpontaneousMessage(day);
    }

    private static long gameTime(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    private static boolean eligible(SocialContact contact, long day, ServerPlayer player) {
        if (contact.isBaby() || DeceasedVillagers.get(player.server).contains(contact.getUuid())) return false;
        if (contact.isMinor() && !contact.isFamily()) return false;
        if (!contact.isFamily() && !contact.isSpouse() && contact.getAffinity() < 25) return false;
        return contact.getLastSpontaneousDay() < 0
                || day - contact.getLastSpontaneousDay() >= CONTACT_COOLDOWN_DAYS;
    }

    private static int weight(SocialContact contact) {
        if (contact.isSpouse()) return 6;
        if (contact.isFamily()) return 5;
        return contact.getAffinity() >= 50 ? 4 : 1;
    }

    private static double chance(SocialContact contact) {
        if (contact.isSpouse()) return 0.35;
        if (contact.isFamily()) return 0.27;
        return contact.getAffinity() >= 50 ? 0.18 : 0.025;
    }

    public static String role(SocialContact contact) {
        if (contact.isSpouse()) return "spouse";
        if (contact.isFamily()) {
            return switch (contact.getAgeStage()) {
                case BABY, CHILD -> "family_child";
                case TEEN -> "family_teen";
                default -> "family_adult";
            };
        }
        return contact.getAffinity() >= 50 ? "friend" : "acquaintance";
    }

    public static String greetingKey(ServerPlayer player, SocialContact contact, UUID messageId) {
        String prefix = "chat.mcasocial.spontaneous.greeting.";
        if (contact.isSpouse()) return prefix + "spouse." + (Math.floorMod(messageId.hashCode(), 2) + 1);
        if (contact.isFamily()) return SocialKinship.greeting(player, contact, messageId);
        return "";
    }

    private static String deliveryDialogueKey(ServerPlayer player, SocialContact contact, String originalKey) {
        if (contact == null) return originalKey;
        String dimension = player.level().dimension().equals(Level.NETHER) ? "nether"
                : player.level().dimension().equals(Level.END) ? "end" : null;
        if (dimension == null) return originalKey;

        String audience = role(contact);
        if (audience.equals("family_child")) audience += parentSuffix(player);
        int variant = ThreadLocalRandom.current().nextInt(1, 3);
        return "chat.mcasocial.spontaneous." + dimension + "." + audience + "." + variant;
    }

    private static String parentSuffix(ServerPlayer player) {
        try {
            var playerData = forge.net.conczin.mca.server.world.data.PlayerSaveData.get(player);
            if (playerData != null && playerData.getGender().toString().equalsIgnoreCase("FEMALE")) {
                return ".mama";
            }
        } catch (Exception ignored) {}
        return ".papa";
    }

    private static int variant(SocialContact contact) {
        String personality = GiftPreferenceProfile.normalizePersonality(contact.getPersonality());
        int preferred = switch (personality) {
            case "timido", "leal" -> 1;
            case "extrovertido", "sarcastico", "chismoso" -> 2;
            case "romantico" -> 3;
            default -> ThreadLocalRandom.current().nextInt(1, 4);
        };
        return preferred == contact.getLastSpontaneousVariant() ? preferred % 3 + 1 : preferred;
    }

    private static boolean hasAnyDevice(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (isDevice(player.getInventory().getItem(i))) return true;
        }
        return false;
    }

    private static boolean isDevice(ItemStack stack) {
        return stack.is(ModItems.SMARTPHONE.get()) || stack.is(ModItems.CARTA.get())
                || stack.is(ModItems.CRYSTAL.get()) || stack.is(ModItems.MAGIC_MIRROR.get());
    }

    private static int availableInterface(ServerPlayer player, int preferred) {
        int mainHand = interfaceFor(player.getMainHandItem());
        if (mainHand >= 0) return mainHand;
        int offHand = interfaceFor(player.getOffhandItem());
        if (offHand >= 0) return offHand;
        int normalized = SocialInterfaceType.normalize(preferred);
        if (hasItem(player, itemFor(normalized))) return normalized;
        if (hasItem(player, ModItems.SMARTPHONE.get())) return SocialInterfaceType.PHONE;
        if (hasItem(player, ModItems.CARTA.get())) return SocialInterfaceType.LETTER;
        if (hasItem(player, ModItems.CRYSTAL.get())) return SocialInterfaceType.CRYSTAL;
        return SocialInterfaceType.MAGIC_MIRROR;
    }

    private static int interfaceFor(ItemStack stack) {
        if (stack.is(ModItems.SMARTPHONE.get())) return SocialInterfaceType.PHONE;
        if (stack.is(ModItems.CARTA.get())) return SocialInterfaceType.LETTER;
        if (stack.is(ModItems.CRYSTAL.get())) return SocialInterfaceType.CRYSTAL;
        if (stack.is(ModItems.MAGIC_MIRROR.get())) return SocialInterfaceType.MAGIC_MIRROR;
        return -1;
    }

    private static Item itemFor(int interfaceType) {
        return switch (interfaceType) {
            case SocialInterfaceType.LETTER -> ModItems.CARTA.get();
            case SocialInterfaceType.CRYSTAL -> ModItems.CRYSTAL.get();
            case SocialInterfaceType.MAGIC_MIRROR -> ModItems.MAGIC_MIRROR.get();
            default -> ModItems.SMARTPHONE.get();
        };
    }

    private static boolean hasItem(ServerPlayer player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(item)) return true;
        }
        return false;
    }
}
