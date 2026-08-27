package com.joelj.mcasocial.social;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SocialContactsProvider implements ICapabilitySerializable<CompoundTag> {
    private final SocialContacts contacts = new SocialContacts();
    private final LazyOptional<ISocialContacts> optional = LazyOptional.of(() -> contacts);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull net.minecraftforge.common.capabilities.Capability<T> cap,
                                             @Nullable Direction side) {
        return cap == SocialContactsCapability.SOCIAL_CONTACTS ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return contacts.save();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        contacts.load(nbt);
    }
}
