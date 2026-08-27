package com.joelj.mcasocial.network;

import com.joelj.mcasocial.item.ModItems;
import com.joelj.mcasocial.social.SocialContactsCapability;
import com.joelj.mcasocial.social.SocialInterfaceType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestContactsPacket {
    private final int interfaceType;

    public RequestContactsPacket(int interfaceType) {
        this.interfaceType = SocialInterfaceType.normalize(interfaceType);
    }

    public RequestContactsPacket(FriendlyByteBuf buf) {
        this.interfaceType = SocialInterfaceType.normalize(buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.interfaceType);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !hasSocialDevice(player)) return;

            player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).ifPresent(contacts ->
                    ModMessages.sendToPlayer(new OpenContactsScreenPacket(contacts.getContacts(), this.interfaceType), player)
            );
        });
        context.setPacketHandled(true);
        return true;
    }

    public static boolean hasSocialDevice(ServerPlayer player) {
        return isSocialDevice(player.getMainHandItem()) || isSocialDevice(player.getOffhandItem());
    }

    private static boolean isSocialDevice(ItemStack stack) {
        return stack.is(ModItems.SMARTPHONE.get()) || stack.is(ModItems.CARTA.get())
                || stack.is(ModItems.CRYSTAL.get());
    }
}
