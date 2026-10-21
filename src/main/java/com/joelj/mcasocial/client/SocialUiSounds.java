package com.joelj.mcasocial.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

public final class SocialUiSounds {
    private SocialUiSounds() {}

    public static void playOpen(SocialInterfaceTheme theme) {
        if (Minecraft.getInstance().player == null) return;
        switch (theme) {
            case PHONE -> Minecraft.getInstance().player.playSound(SoundEvents.NOTE_BLOCK_HAT.get(), 0.35F, 1.4F);
            case LETTER -> Minecraft.getInstance().player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.55F, 0.9F);
            case CRYSTAL, MAGIC_MIRROR -> Minecraft.getInstance().player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.55F, 1.25F);
        }
    }
}
