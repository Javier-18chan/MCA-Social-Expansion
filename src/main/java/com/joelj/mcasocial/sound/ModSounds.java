package com.joelj.mcasocial.sound;

import com.joelj.mcasocial.MCASocialExpansion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MCASocialExpansion.MODID);

    public static final RegistryObject<SoundEvent> MESSAGE_SENT = register("message.sent");
    public static final RegistryObject<SoundEvent> PHONE_RECEIVED = register("message.phone_received");
    public static final RegistryObject<SoundEvent> LETTER_RECEIVED = register("message.letter_received");
    public static final RegistryObject<SoundEvent> CRYSTAL_RECEIVED = register("message.crystal_received");

    private ModSounds() {}

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(location));
    }
}
