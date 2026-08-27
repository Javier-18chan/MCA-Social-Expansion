package com.joelj.mcasocial.social;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SocialProvider implements ICapabilitySerializable<CompoundTag> {

    private final SocialData data = new SocialData();
    private final LazyOptional<ISocialData> optional = LazyOptional.of(() -> data);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull net.minecraftforge.common.capabilities.Capability<T> cap,
                                             @Nullable Direction side) {
        return cap == SocialCapability.SOCIAL_CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Affinity", data.getAffinity());
        tag.putInt("Romance", data.getRomance());
        tag.putInt("Trust", data.getTrust());
        tag.putInt("Reputation", data.getReputation());
        tag.putString("Personality", data.getPersonality());

        tag.putBoolean("HasUnlockedLetter", data.hasUnlockedLetter());
        tag.putInt("SelectedInterfaceId", data.getSelectedInterfaceId());

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.setAffinity(nbt.getInt("Affinity"));
        if (nbt.contains("Romance")) {
            data.setRomance(nbt.getInt("Romance"));
        }
        data.setTrust(nbt.getInt("Trust"));
        data.setReputation(nbt.getInt("Reputation"));
        data.setPersonality(nbt.getString("Personality"));

        data.setHasUnlockedLetter(nbt.getBoolean("HasUnlockedLetter"));
        data.setSelectedInterfaceId(nbt.getInt("SelectedInterfaceId"));
    }
}
