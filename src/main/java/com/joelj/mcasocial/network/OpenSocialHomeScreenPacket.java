package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.SocialInterfaceType;
import com.joelj.mcasocial.social.DeliveryDelayMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenSocialHomeScreenPacket {
    public final int interfaceType;
    public final DeliveryDelayMode delayMode;
    public final int pendingCount;
    public final boolean spontaneousEnabled;

    public OpenSocialHomeScreenPacket(int interfaceType) {
        this(interfaceType, DeliveryDelayMode.SHORT, 0, true);
    }

    public OpenSocialHomeScreenPacket(int interfaceType, DeliveryDelayMode delayMode, int pendingCount) {
        this(interfaceType, delayMode, pendingCount, true);
    }

    public OpenSocialHomeScreenPacket(int interfaceType, DeliveryDelayMode delayMode, int pendingCount,
                                      boolean spontaneousEnabled) {
        this.interfaceType = SocialInterfaceType.normalize(interfaceType);
        this.delayMode = delayMode == null ? DeliveryDelayMode.SHORT : delayMode;
        this.pendingCount = Math.max(0, pendingCount);
        this.spontaneousEnabled = spontaneousEnabled;
    }

    public OpenSocialHomeScreenPacket(FriendlyByteBuf buf) {
        this.interfaceType = SocialInterfaceType.normalize(buf.readInt());
        this.delayMode = DeliveryDelayMode.fromOrdinal(buf.readInt());
        this.pendingCount = Math.max(0, buf.readInt());
        this.spontaneousEnabled = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.interfaceType);
        buf.writeInt(this.delayMode.ordinal());
        buf.writeInt(this.pendingCount);
        buf.writeBoolean(this.spontaneousEnabled);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.joelj.mcasocial.client.ClientScreenOpener.openHome(this))
        );
        context.setPacketHandled(true);
        return true;
    }
}
