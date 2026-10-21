package com.joelj.mcasocial.social;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "mcasocial")
public final class SocialContactCommands {
    private SocialContactCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mcasocial")
                .then(Commands.literal("forget")
                        .then(Commands.argument("contact", StringArgumentType.greedyString())
                                .suggests((context, builder) -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).ifPresent(contacts ->
                                                contacts.getContacts().forEach(contact -> {
                                                    if (contact.getName().toLowerCase(Locale.ROOT).startsWith(
                                                            builder.getRemainingLowerCase())) {
                                                        builder.suggest(contact.getName());
                                                    }
                                                    if (contact.getUuid().toString().startsWith(
                                                            builder.getRemainingLowerCase())) {
                                                        builder.suggest(contact.getUuid().toString());
                                                    }
                                                }));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> forget(context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "contact"))))));
    }

    private static int forget(ServerPlayer player, String identifier) {
        ISocialContacts contacts = player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).orElse(null);
        if (contacts == null) return 0;

        SocialContact contact = null;
        try {
            contact = contacts.getContact(UUID.fromString(identifier));
        } catch (IllegalArgumentException ignored) {
        }
        if (contact == null) {
            List<SocialContact> matches = contacts.getContacts().stream()
                    .filter(candidate -> candidate.getName().equalsIgnoreCase(identifier)).toList();
            if (matches.size() > 1) {
                player.sendSystemMessage(Component.translatable("chat.mcasocial.contacts.ambiguous"));
                return 0;
            }
            if (!matches.isEmpty()) contact = matches.get(0);
        }
        if (contact == null) {
            player.sendSystemMessage(Component.translatable("chat.mcasocial.contacts.not_found"));
            return 0;
        }

        contacts.removeContact(contact.getUuid());
        player.sendSystemMessage(Component.translatable("chat.mcasocial.contacts.forgot", contact.getName()));
        return 1;
    }
}
