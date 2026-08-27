package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.SocialInterfaceType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenSocialHomeScreenPacket {
    public final int interfaceType;

    public OpenSocialHomeScreenPacket(int interfaceType) {
        this.interfaceType = SocialInterfaceType.normalize(interfaceType);
    }

    public OpenSocialHomeScreenPacket(FriendlyByteBuf buf) {
        this.interfaceType = SocialInterfaceType.normalize(buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.interfaceType);
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
