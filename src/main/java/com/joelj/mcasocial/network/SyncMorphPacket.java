package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.SocialInterfaceType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

import java.util.function.Supplier;

public class SyncMorphPacket {
    public final int targetInterface;

    public SyncMorphPacket(int targetInterface) {
        this.targetInterface = SocialInterfaceType.normalize(targetInterface);
    }

    public SyncMorphPacket(FriendlyByteBuf buf) {
        int interfaceId = buf.readInt();
        this.targetInterface = SocialInterfaceType.normalize(interfaceId);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.targetInterface);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {

            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.joelj.mcasocial.client.ClientScreenOpener.syncMorph(this));
        });
        context.setPacketHandled(true);
        return true;
    }
}
