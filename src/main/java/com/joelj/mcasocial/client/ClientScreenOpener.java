package com.joelj.mcasocial.client;

import com.joelj.mcasocial.network.OpenContactsScreenPacket;
import com.joelj.mcasocial.network.OpenSocialHomeScreenPacket;
import com.joelj.mcasocial.network.OpenSocialScreenPacket;
import com.joelj.mcasocial.network.SyncMorphPacket;
import com.joelj.mcasocial.network.OpenInboxScreenPacket;
import com.joelj.mcasocial.social.SocialInterfaceType;
import net.minecraft.client.Minecraft;

public class ClientScreenOpener {

    public static void open(OpenSocialScreenPacket data) {
        ClientSocialProfile.update(data);
        if (data.interfaceType == SocialInterfaceType.LETTER) {
            Minecraft.getInstance().setScreen(new CardSocialScreen(
                    data.entityId, data.nombre, data.personalidad, data.afinidad,
                    data.romance, data.trust, data.isBaby, data.isMinor, data.isFamily, data.isSpouse,
                    data.ageStage, data.contactUuid
            ));
        } else if (data.interfaceType == SocialInterfaceType.MAGIC_MIRROR) {
            Minecraft.getInstance().setScreen(new MagicMirrorSocialScreen(
                    data.entityId, data.nombre, data.personalidad, data.afinidad,
                    data.romance, data.trust, data.isBaby, data.isMinor, data.isFamily, data.isSpouse,
                    data.ageStage, data.contactUuid
            ));
        } else if (data.interfaceType == SocialInterfaceType.CRYSTAL) {
            Minecraft.getInstance().setScreen(new CrystalSocialScreen(
                    data.entityId, data.nombre, data.personalidad, data.afinidad,
                    data.romance, data.trust, data.isBaby, data.isMinor, data.isFamily, data.isSpouse,
                    data.ageStage, data.contactUuid
            ));
        } else {
            Minecraft.getInstance().setScreen(new SocialScreen(
                    data.entityId, data.nombre, data.personalidad, data.afinidad,
                    data.romance, data.trust, data.isBaby, data.isMinor, data.isFamily, data.isSpouse,
                    data.ageStage, data.contactUuid
            ));
        }
    }

    public static void openContacts(OpenContactsScreenPacket data) {
        Minecraft.getInstance().setScreen(new ContactListScreen(data.contacts, data.interfaceType));
    }

    public static void openHome(OpenSocialHomeScreenPacket data) {
        ClientSocialSettings.setSpontaneousEnabled(data.spontaneousEnabled);
        Minecraft.getInstance().setScreen(new SocialHomeScreen(data.interfaceType, data.delayMode, data.pendingCount));
    }

    public static void openInbox(OpenInboxScreenPacket data) {
        Minecraft.getInstance().setScreen(new InboxScreen(data.entries, data.incomingEntries,
                data.interfaceType, data.delayMode));
    }

    public static void syncMorph(SyncMorphPacket data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof SocialScreen s) {
            if (data.targetInterface == SocialInterfaceType.LETTER) s.morphToCard();
            else if (data.targetInterface == SocialInterfaceType.CRYSTAL) s.morphToCrystal();
        } else if (mc.screen instanceof CardSocialScreen c) {
            if (data.targetInterface == SocialInterfaceType.PHONE) c.morphToPhone();
            else if (data.targetInterface == SocialInterfaceType.CRYSTAL) c.morphToCrystal();
        } else if (mc.screen instanceof CrystalSocialScreen crystal) {
            if (data.targetInterface == SocialInterfaceType.PHONE) crystal.morphToPhone();
            else if (data.targetInterface == SocialInterfaceType.LETTER) crystal.morphToCard();
            else if (data.targetInterface == SocialInterfaceType.MAGIC_MIRROR) crystal.morphToMagicMirror();
        } else if (mc.screen instanceof InterfaceSettingsScreen settings) {
            settings.openAfterMorph(data.targetInterface);
        }
    }
}
