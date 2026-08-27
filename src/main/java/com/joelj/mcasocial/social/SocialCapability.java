package com.joelj.mcasocial.social;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class SocialCapability {

    public static final Capability<ISocialData> SOCIAL_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

}
