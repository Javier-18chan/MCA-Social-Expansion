package com.joelj.mcasocial.network;

import com.joelj.mcasocial.MCASocialExpansion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static final String PROTOCOL_VERSION = "2.3";
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() { return packetId++; }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "messages"))
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(SocialInteractPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(SocialInteractPacket::new).encoder(SocialInteractPacket::toBytes).consumerMainThread(SocialInteractPacket::handle).add();
        net.messageBuilder(OpenSocialScreenPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT).decoder(OpenSocialScreenPacket::new).encoder(OpenSocialScreenPacket::toBytes).consumerMainThread(OpenSocialScreenPacket::handle).add();
        net.messageBuilder(GiftInteractPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(GiftInteractPacket::new).encoder(GiftInteractPacket::toBytes).consumerMainThread(GiftInteractPacket::handle).add();

        net.messageBuilder(MorphInterfacePacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(MorphInterfacePacket::new).encoder(MorphInterfacePacket::toBytes).consumerMainThread(MorphInterfacePacket::handle).add();
        net.messageBuilder(SyncMorphPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT).decoder(SyncMorphPacket::new).encoder(SyncMorphPacket::toBytes).consumerMainThread(SyncMorphPacket::handle).add();
        net.messageBuilder(RequestContactsPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(RequestContactsPacket::new).encoder(RequestContactsPacket::toBytes).consumerMainThread(RequestContactsPacket::handle).add();
        net.messageBuilder(OpenContactsScreenPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT).decoder(OpenContactsScreenPacket::new).encoder(OpenContactsScreenPacket::toBytes).consumerMainThread(OpenContactsScreenPacket::handle).add();
        net.messageBuilder(RequestOpenContactPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(RequestOpenContactPacket::new).encoder(RequestOpenContactPacket::toBytes).consumerMainThread(RequestOpenContactPacket::handle).add();
        net.messageBuilder(ContactInteractPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(ContactInteractPacket::new).encoder(ContactInteractPacket::toBytes).consumerMainThread(ContactInteractPacket::handle).add();
        net.messageBuilder(ToggleContactFavoritePacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(ToggleContactFavoritePacket::new).encoder(ToggleContactFavoritePacket::toBytes).consumerMainThread(ToggleContactFavoritePacket::handle).add();
        net.messageBuilder(OpenSocialHomeScreenPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT).decoder(OpenSocialHomeScreenPacket::new).encoder(OpenSocialHomeScreenPacket::toBytes).consumerMainThread(OpenSocialHomeScreenPacket::handle).add();
        net.messageBuilder(UpdateSocialSettingsPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(UpdateSocialSettingsPacket::new).encoder(UpdateSocialSettingsPacket::toBytes).consumerMainThread(UpdateSocialSettingsPacket::handle).add();
        net.messageBuilder(RequestInboxPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(RequestInboxPacket::new).encoder(RequestInboxPacket::toBytes).consumerMainThread(RequestInboxPacket::handle).add();
        net.messageBuilder(OpenInboxScreenPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT).decoder(OpenInboxScreenPacket::new).encoder(OpenInboxScreenPacket::toBytes).consumerMainThread(OpenInboxScreenPacket::handle).add();
        net.messageBuilder(ReplySpontaneousMessagePacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(ReplySpontaneousMessagePacket::new).encoder(ReplySpontaneousMessagePacket::toBytes).consumerMainThread(ReplySpontaneousMessagePacket::handle).add();
    }

    public static <MSG> void sendToServer(MSG message) { INSTANCE.sendToServer(message); }
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) { INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message); }
}
