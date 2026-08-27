package com.joelj.mcasocial.social;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class SocialContactsCapability {
    public static final Capability<ISocialContacts> SOCIAL_CONTACTS =
            CapabilityManager.get(new CapabilityToken<>() {});
}
