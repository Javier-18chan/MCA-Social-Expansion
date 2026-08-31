package com.joelj.mcasocial.social;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class SocialInterfaceSounds {
    private SocialInterfaceSounds() {}

    public static void playSent(ServerPlayer player, int interfaceType) {
        play(player, interfaceType, 0.45F, interfaceType == SocialInterfaceType.LETTER ? 0.9F : 1.2F);
    }

    public static void playDelivered(ServerPlayer player, int interfaceType) {
        play(player, interfaceType, 0.6F, interfaceType == SocialInterfaceType.CRYSTAL ? 1.35F : 1.0F);
    }

    public static SoundEvent soundFor(int interfaceType) {
        return switch (SocialInterfaceType.normalize(interfaceType)) {
            case SocialInterfaceType.LETTER -> SoundEvents.BOOK_PAGE_TURN;
            case SocialInterfaceType.CRYSTAL -> SoundEvents.AMETHYST_BLOCK_CHIME;
            default -> SoundEvents.NOTE_BLOCK_HAT.get();
        };
    }

    public static void playResult(ServerPlayer player, SocialActionService.InteractionChannel channel, float pitch) {
        if (channel == SocialActionService.InteractionChannel.FACE_TO_FACE) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.5F, pitch);
            return;
        }
        int interfaceType = switch (channel) {
            case LETTER -> SocialInterfaceType.LETTER;
            case CRYSTAL -> SocialInterfaceType.CRYSTAL;
            default -> SocialInterfaceType.PHONE;
        };
        playDelivered(player, interfaceType);
    }

    private static void play(ServerPlayer player, int interfaceType, float volume, float pitch) {
        player.level().playSound(null, player.blockPosition(), soundFor(interfaceType), SoundSource.PLAYERS, volume, pitch);
    }
}
