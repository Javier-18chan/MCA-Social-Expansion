package com.joelj.mcasocial.network;

import com.joelj.mcasocial.item.ModItems;
import com.joelj.mcasocial.social.SocialInterfaceType;
import com.joelj.mcasocial.social.SocialContactsCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MorphInterfacePacket {
    private final int targetInterface;

    public MorphInterfacePacket(int targetInterface) {
        this.targetInterface = SocialInterfaceType.normalize(targetInterface);
    }

    public MorphInterfacePacket(FriendlyByteBuf buf) {
        this.targetInterface = SocialInterfaceType.normalize(buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.targetInterface);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            ItemStack heldSocialItem = getHeldSocialItem(player);
            if (heldSocialItem.isEmpty() || interfaceId(heldSocialItem) == this.targetInterface) return;

            if (this.targetInterface == SocialInterfaceType.LETTER) {

                if (hasItems(player, Items.PAPER, 4) && hasItems(player, Items.LEATHER, 1)) {
                    quitarItems(player, Items.PAPER, 4);
                    quitarItems(player, Items.LEATHER, 1);
                    swapHeldSocialItem(player, ModItems.CARTA.get());

                    player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
                    ModMessages.sendToPlayer(new SyncMorphPacket(SocialInterfaceType.LETTER), player);
                } else {
                    player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.VILLAGER_NO, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            } else if (this.targetInterface == SocialInterfaceType.PHONE) {

                if (hasItems(player, Items.IRON_INGOT, 7) && hasItems(player, Items.REDSTONE, 1) && hasItems(player, Items.GLASS_PANE, 1)) {
                    quitarItems(player, Items.IRON_INGOT, 7);
                    quitarItems(player, Items.REDSTONE, 1);
                    quitarItems(player, Items.GLASS_PANE, 1);
                    swapHeldSocialItem(player, ModItems.SMARTPHONE.get());

                    player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
                    ModMessages.sendToPlayer(new SyncMorphPacket(SocialInterfaceType.PHONE), player);
                } else {
                    player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.VILLAGER_NO, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            } else if (this.targetInterface == SocialInterfaceType.CRYSTAL) {
                if (hasItems(player, Items.AMETHYST_SHARD, 3) && hasItems(player, Items.DIAMOND, 1)
                        && hasItems(player, Items.ENDER_PEARL, 1)) {
                    quitarItems(player, Items.AMETHYST_SHARD, 3);
                    quitarItems(player, Items.DIAMOND, 1);
                    quitarItems(player, Items.ENDER_PEARL, 1);
                    swapHeldSocialItem(player, ModItems.CRYSTAL.get());

                    player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.2F);
                    ModMessages.sendToPlayer(new SyncMorphPacket(SocialInterfaceType.CRYSTAL), player);
                } else {
                    player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.VILLAGER_NO, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            } else if (this.targetInterface == SocialInterfaceType.MAGIC_MIRROR) {
                if (hasItems(player, Items.GOLD_INGOT, 6)
                        && hasItems(player, Items.AMETHYST_SHARD, 1)
                        && hasItems(player, Items.GLASS_PANE, 1)
                        && hasItems(player, Items.REDSTONE, 1)) {
                    quitarItems(player, Items.GOLD_INGOT, 6);
                    quitarItems(player, Items.AMETHYST_SHARD, 1);
                    quitarItems(player, Items.GLASS_PANE, 1);
                    quitarItems(player, Items.REDSTONE, 1);
                    swapHeldSocialItem(player, ModItems.MAGIC_MIRROR.get());

                    player.level().playSound(null, player.blockPosition(),
                            net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                            net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.0F);
                    ModMessages.sendToPlayer(new SyncMorphPacket(SocialInterfaceType.MAGIC_MIRROR), player);
                } else {
                    player.level().playSound(null, player.blockPosition(),
                            net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        });
        context.setPacketHandled(true);
        return true;
    }

    private boolean hasItems(ServerPlayer player, net.minecraft.world.item.Item item, int amount) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) count += stack.getCount();
        }
        return count >= amount;
    }

    private void quitarItems(ServerPlayer player, net.minecraft.world.item.Item item, int cantidadAQuitar) {
        int restantes = cantidadAQuitar;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                if (stack.getCount() >= restantes) {
                    stack.shrink(restantes);
                    return;
                } else {
                    restantes -= stack.getCount();
                    stack.setCount(0);
                }
            }
        }
    }

    private ItemStack getHeldSocialItem(ServerPlayer player) {
        if (isSocialItem(player.getMainHandItem())) return player.getMainHandItem();
        if (isSocialItem(player.getOffhandItem())) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    private boolean isSocialItem(ItemStack stack) {
        return stack.is(ModItems.SMARTPHONE.get()) || stack.is(ModItems.CARTA.get())
                || stack.is(ModItems.CRYSTAL.get()) || stack.is(ModItems.MAGIC_MIRROR.get());
    }

    private int interfaceId(ItemStack stack) {
        if (stack.is(ModItems.CARTA.get())) return SocialInterfaceType.LETTER;
        if (stack.is(ModItems.CRYSTAL.get())) return SocialInterfaceType.CRYSTAL;
        if (stack.is(ModItems.MAGIC_MIRROR.get())) return SocialInterfaceType.MAGIC_MIRROR;
        return SocialInterfaceType.PHONE;
    }

    private void swapHeldSocialItem(ServerPlayer player, net.minecraft.world.item.Item newItem) {
        if (isSocialItem(player.getMainHandItem())) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(newItem));
        } else if (isSocialItem(player.getOffhandItem())) {
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(newItem));
        }
        player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).ifPresent(contacts ->
                contacts.setPreferredInterfaceId(interfaceId(new ItemStack(newItem))));
    }
}
