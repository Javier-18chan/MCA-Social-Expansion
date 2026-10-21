package com.joelj.mcasocial.social;

import com.joelj.mcasocial.sound.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class SocialInterfaceSounds {
    private SocialInterfaceSounds() {}

    public static void playSent(ServerPlayer player, int interfaceType) {
        player.playNotifySound(ModSounds.MESSAGE_SENT.get(), SoundSource.PLAYERS, 0.45F, 1.0F);
    }

    public static void playDelivered(ServerPlayer player, int interfaceType) {
        play(player, interfaceType, 0.6F, 1.0F);
    }

    public static SoundEvent soundFor(int interfaceType) {
        return switch (SocialInterfaceType.normalize(interfaceType)) {
            case SocialInterfaceType.LETTER -> ModSounds.LETTER_RECEIVED.get();
            case SocialInterfaceType.CRYSTAL, SocialInterfaceType.MAGIC_MIRROR -> ModSounds.CRYSTAL_RECEIVED.get();
            default -> ModSounds.PHONE_RECEIVED.get();
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
        player.playNotifySound(soundFor(interfaceType), SoundSource.PLAYERS, volume, pitch);
    }
}
