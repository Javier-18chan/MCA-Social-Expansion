package com.joelj.mcasocial;

import com.joelj.mcasocial.item.ModItems;
import com.joelj.mcasocial.network.ModMessages;
import com.joelj.mcasocial.network.OpenSocialHomeScreenPacket;
import com.joelj.mcasocial.network.OpenSocialScreenPacket;
import com.joelj.mcasocial.social.ISocialData;
import com.joelj.mcasocial.social.ISocialContacts;
import com.joelj.mcasocial.social.McaRomanceSync;
import com.joelj.mcasocial.social.SocialCapability;
import com.joelj.mcasocial.social.SocialActionService;
import com.joelj.mcasocial.social.SocialContact;
import com.joelj.mcasocial.social.SocialContactHelper;
import com.joelj.mcasocial.social.SocialContactsCapability;
import com.joelj.mcasocial.social.SocialContactsProvider;
import com.joelj.mcasocial.social.SocialInterfaceType;
import com.joelj.mcasocial.social.SocialProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import forge.net.conczin.mca.entity.VillagerEntityMCA;

@Mod.EventBusSubscriber(modid = "mcasocial")
public class SocialEvents {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "social_data");
    private static final ResourceLocation CONTACTS_ID = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "social_contacts");

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof Player) {
            event.addCapability(CONTACTS_ID, new SocialContactsProvider());
        }

        ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityKey == null) return;
        String entityName = entityKey.toString();
        if (entityName.equals("mca:female_villager") || entityName.equals("mca:male_villager")) {
            event.addCapability(ID, new SocialProvider());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();

        net.minecraft.world.item.Item handItem = player.getMainHandItem().getItem();
        if (handItem != ModItems.SMARTPHONE.get() && handItem != ModItems.CARTA.get()
                && handItem != ModItems.CRYSTAL.get()) return;

        int interfaceType = handItem == ModItems.CARTA.get()
                ? SocialInterfaceType.LETTER
                : handItem == ModItems.CRYSTAL.get() ? SocialInterfaceType.CRYSTAL : SocialInterfaceType.PHONE;

        Entity entity = event.getTarget();

        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {

            if (entity instanceof net.minecraft.world.entity.Mob mob) {
                mob.getNavigation().stop();
                mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
                mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.entity.player.Player.class.equals(player.getClass()) ? net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN : net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 10, false, false));
            }

            entity.getCapability(SocialCapability.SOCIAL_CAPABILITY).ifPresent(data -> {

                boolean isBabyFlag = false;
                boolean isMinorFlag = false;
                boolean isFamilyFlag = false;
                boolean isSpouseFlag = false;

                if (entity instanceof net.minecraft.world.entity.AgeableMob ageable) {
                    isBabyFlag = ageable.isBaby();
                }

                if (entity instanceof VillagerEntityMCA mcaVillager) {
                    try {
                        java.lang.reflect.Method getAgeStateMethod = mcaVillager.getClass().getMethod("getAgeState");
                        Object ageStateObj = getAgeStateMethod.invoke(mcaVillager);
                        String etapa = ageStateObj.toString();

                        isBabyFlag = etapa.equals("BABY");
                        isMinorFlag = !etapa.equals("ADULT");

                        ISocialContacts contacts = serverPlayer
                                .getCapability(SocialContactsCapability.SOCIAL_CONTACTS)
                                .orElse(null);
                        SocialContact storedContact = contacts == null
                                ? null
                                : contacts.getContact(entity.getUUID());
                        McaRomanceSync.reconcile(mcaVillager, serverPlayer, data, storedContact);

                        boolean enArbolFamiliar = false;
                        boolean esMiPareja = false;

                        try {
                            net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
                            mcaVillager.saveWithoutId(nbt);
                            String miUUID = player.getUUID().toString();

                            if (nbt.contains("fatherUUID") && nbt.getString("fatherUUID").equals(miUUID)) enArbolFamiliar = true;
                            if (nbt.contains("motherUUID") && nbt.getString("motherUUID").equals(miUUID)) enArbolFamiliar = true;
                            if (nbt.contains("father") && nbt.getString("father").equals(miUUID)) enArbolFamiliar = true;
                            if (nbt.contains("mother") && nbt.getString("mother").equals(miUUID)) enArbolFamiliar = true;
                            if (nbt.contains("parents") && nbt.get("parents").toString().contains(miUUID)) enArbolFamiliar = true;

                            if (nbt.contains("memories")) {
                                net.minecraft.nbt.CompoundTag memoriesTag = nbt.getCompound("memories");
                                if (memoriesTag.contains(miUUID)) {
                                    net.minecraft.nbt.CompoundTag myMemory = memoriesTag.getCompound(miUUID);
                                    if (myMemory.contains("dialogueType")) {
                                        int dialogueType = myMemory.getInt("dialogueType");
                                        if (dialogueType == 2) {
                                            enArbolFamiliar = true;
                                        } else if (dialogueType == 1) {
                                            esMiPareja = true;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {}

                        try {
                            net.minecraft.world.SimpleContainer inv = mcaVillager.getInventory();
                            for (int i = 0; i < inv.getContainerSize(); i++) {
                                String itemName = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(inv.getItem(i).getItem()).toString();
                                if (itemName.contains("wedding_ring") && data.getRomance() >= 50) {
                                    esMiPareja = true;
                                    break;
                                }
                            }
                        } catch (Exception e) {}

                        isSpouseFlag = esMiPareja;
                        isFamilyFlag = enArbolFamiliar && !esMiPareja;

                    } catch (Exception e) {
                        if (isBabyFlag) isMinorFlag = true;
                    }
                }

                boolean previousFamilyFlag = isFamilyFlag;
                boolean previousSpouseFlag = isSpouseFlag;
                SocialActionService.RoleInfo detectedRole = SocialActionService.inspectRole(entity, serverPlayer, previousFamilyFlag);
                isBabyFlag = detectedRole.isBaby();
                isMinorFlag = detectedRole.isMinor();
                isSpouseFlag = detectedRole.isSpouse() || previousSpouseFlag;
                isFamilyFlag = (detectedRole.isFamily() || previousFamilyFlag) && !isSpouseFlag;

                int idAldeano = entity.getId();
                String nombreAldeano = entity.getName().getString();
                String personalidad = data.getPersonality();
                int afinidad = data.getAffinity();
                int romance = data.getRomance();

                SocialContactHelper.remember(serverPlayer, entity, data, isBabyFlag, isMinorFlag,
                        detectedRole.ageStage(), isFamilyFlag, isSpouseFlag);

                ModMessages.sendToPlayer(
                        new OpenSocialScreenPacket(idAldeano, nombreAldeano, personalidad, afinidad, romance,
                                data.getTrust(), isBabyFlag, isMinorFlag, isFamilyFlag, isSpouseFlag,
                                detectedRole.ageStage(), interfaceType, null),
                        serverPlayer
                );
            });
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Item item = event.getItemStack().getItem();
        if (item != ModItems.SMARTPHONE.get() && item != ModItems.CARTA.get()
                && item != ModItems.CRYSTAL.get()) return;

        int interfaceType = item == ModItems.CARTA.get()
                ? SocialInterfaceType.LETTER
                : item == ModItems.CRYSTAL.get() ? SocialInterfaceType.CRYSTAL : SocialInterfaceType.PHONE;
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new OpenSocialHomeScreenPacket(interfaceType), serverPlayer);
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(SocialContactsCapability.SOCIAL_CONTACTS).ifPresent(oldContacts ->
                event.getEntity().getCapability(SocialContactsCapability.SOCIAL_CONTACTS).ifPresent(newContacts ->
                        newContacts.copyFrom(oldContacts)
                )
        );
        event.getOriginal().invalidateCaps();
    }

    @Mod.EventBusSubscriber(modid = "mcasocial", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerCaps(RegisterCapabilitiesEvent event) {
            event.register(ISocialData.class);
            event.register(ISocialContacts.class);
        }
    }
}
