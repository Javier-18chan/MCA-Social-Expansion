package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.SocialCapability;
import forge.net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.function.Supplier;

public class GiftInteractPacket {
    private static final int MAX_ITEM_ID_LENGTH = 128;
    private static final Set<String> REGALOS_CAROS = Set.of(
            "minecraft:diamond",
            "minecraft:emerald",
            "minecraft:gold_ingot",
            "minecraft:golden_apple",
            "minecraft:enchanted_golden_apple",
            "minecraft:netherite_ingot"
    );
    private static final Set<String> REGALOS_ODIADOS = Set.of(
            "minecraft:rotten_flesh",
            "minecraft:spider_eye",
            "minecraft:bone"
    );

    private final int entityId;
    private final String itemName;
    private final boolean isFamily;

    public GiftInteractPacket(int entityId, String itemName, boolean isFamily) {
        this.entityId = entityId;
        this.itemName = itemName;
        this.isFamily = isFamily;
    }

    public GiftInteractPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.itemName = buf.readUtf(MAX_ITEM_ID_LENGTH);
        this.isFamily = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeUtf(this.itemName);
        buf.writeBoolean(this.isFamily);
    }

    private String getObjetoFavorito(String personalidadLimpia) {
        if (personalidadLimpia == null) return "";
        return switch (personalidadLimpia) {
            case "extrovertido" -> "minecraft:cake";
            case "timido" -> "minecraft:cookie";
            case "ambicioso" -> "minecraft:diamond";
            case "chismoso" -> "minecraft:sweet_berries";
            case "orgulloso" -> "minecraft:emerald";
            case "leal" -> "minecraft:iron_ingot";
            case "sarcastico" -> "minecraft:amethyst_shard";
            case "romantico" -> "minecraft:blue_orchid";
            default -> "";
        };
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (ResourceLocation.tryParse(this.itemName) == null) return;

            Entity entity = player.level().getEntity(this.entityId);
            if (entity != null) {

                boolean esUnBebeTemp = false;
                boolean esMenorTemp = false;
                if (entity instanceof VillagerEntityMCA mcaVillager) {
                    try {
                        java.lang.reflect.Method getAgeStateMethod = mcaVillager.getClass().getMethod("getAgeState");
                        String ageState = getAgeStateMethod.invoke(mcaVillager).toString();
                        if (ageState.equals("BABY")) esUnBebeTemp = true;
                        if (ageState.equals("TODDLER") || ageState.equals("CHILD") || ageState.equals("TEEN")) esMenorTemp = true;
                    } catch (Exception e) {}
                }
                final boolean finalEsUnBebe = esUnBebeTemp;
                final boolean esMenor = esMenorTemp;
                final boolean esMiHijo = this.isFamily;

                int slotEncontrado = -1;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty()) {
                        ResourceLocation registryName = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
                        if (registryName != null && registryName.toString().equals(this.itemName)) {
                            slotEncontrado = i;
                            break;
                        }
                    }
                }

                if (slotEncontrado != -1) {
                    ItemStack itemRegalo = player.getInventory().getItem(slotEncontrado);

                    entity.getCapability(SocialCapability.SOCIAL_CAPABILITY).ifPresent(data -> {
                        String nombreNpc = entity.getName().getString();
                        String personalidad = data.getPersonality();

                        boolean esPareja = data.getRomance() >= 50 && !esMenor && !esMiHijo;

                        long tiempoActual = player.level().getGameTime();
                        long ultimoRegalo = data.getLastActionTime("regalar_dia");
                        int regalosDadosHoy = data.getActionUseCount("contador_regalos");
                        long unDiaEnTicks = 24000L;

                        if (tiempoActual - ultimoRegalo > unDiaEnTicks) {
                            regalosDadosHoy = 0;
                        }

                        if (regalosDadosHoy == 4) {

                            String msjLimite = "chat.mcasocial.regalo.limite";
                            if (esMenor) msjLimite = "chat.mcasocial.global.harto";
                            if (!esMenor && !finalEsUnBebe && esMiHijo) msjLimite = "chat.mcasocial.familiar_adulto.spam";

                            player.sendSystemMessage(Component.literal("§d<" + nombreNpc + "> §f").append(Component.translatable(finalEsUnBebe ? "chat.mcasocial.bebe.insulto" : msjLimite)));
                            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.VILLAGER_NO, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
                            data.setActionUseCount("contador_regalos", 5);
                            data.setLastActionTime("regalar_dia", tiempoActual);
                            return;
                        } else if (regalosDadosHoy >= 5) {
                            player.sendSystemMessage(Component.literal("§d<" + nombreNpc + "> §f").append(Component.translatable(finalEsUnBebe ? "chat.mcasocial.bebe.insulto" : "chat.mcasocial.global.bloqueado")));
                            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.VILLAGER_NO, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
                            return;
                        }

                        int puntosGanados = 0;
                        String llaveDialogo = "";

                        String personalidadLimpia = personalidad == null ? "" : personalidad.toLowerCase().replace("í", "i").replace("á", "a").replace("ó", "o").trim();
                        String objetoObsesion = getObjetoFavorito(personalidadLimpia);

                        if (esMenor && (this.itemName.equals("minecraft:cake") || this.itemName.equals("minecraft:cookie"))) {
                            objetoObsesion = this.itemName;
                        }

                        if (this.itemName.equals(objetoObsesion)) {
                            if (regalosDadosHoy == 0) {
                                puntosGanados = 12;
                                llaveDialogo = "chat.mcasocial.regalo.favorito." + personalidadLimpia;
                                if (esPareja) llaveDialogo += ".pareja";
                            } else {
                                puntosGanados = 5;
                                llaveDialogo = "chat.mcasocial.regalo.sospecha_normal";
                            }
                        } else if (regalosDadosHoy < 2) {
                            if (REGALOS_CAROS.contains(this.itemName)) {
                                puntosGanados = 7;
                                llaveDialogo = esPareja ? "chat.mcasocial.regalo.amado.pareja" : "chat.mcasocial.regalo.amado";
                            } else if (REGALOS_ODIADOS.contains(this.itemName)) {
                                puntosGanados = -5;
                                llaveDialogo = esPareja ? "chat.mcasocial.regalo.odiado.pareja" : "chat.mcasocial.regalo.odiado";
                            } else {
                                puntosGanados = 3;
                                llaveDialogo = esPareja ? "chat.mcasocial.regalo.normal.pareja" : "chat.mcasocial.regalo.normal";
                            }
                        } else {
                            if (REGALOS_CAROS.contains(this.itemName)) {
                                puntosGanados = 2;
                                llaveDialogo = "chat.mcasocial.regalo.sospecha_bueno";
                            } else if (REGALOS_ODIADOS.contains(this.itemName)) {
                                puntosGanados = -8;
                                llaveDialogo = "chat.mcasocial.regalo.sospecha_malo";
                            } else {
                                puntosGanados = 0;
                                llaveDialogo = "chat.mcasocial.regalo.sospecha_normal";
                            }
                        }

                        if (esMenor && !finalEsUnBebe) {
                            String prefijo = esMiHijo ? "chat.mcasocial.hijo.regalo." : "chat.mcasocial.menor.regalo.";

                            if (this.itemName.equals(objetoObsesion) && regalosDadosHoy == 0) {
                                llaveDialogo = prefijo + "favorito";
                            } else if (puntosGanados >= 7) {
                                llaveDialogo = prefijo + "amado";
                            } else if (puntosGanados < 0) {
                                llaveDialogo = prefijo + "odiado";
                            } else {
                                llaveDialogo = prefijo + "normal";
                            }
                        } else if (!esMenor && !finalEsUnBebe && esMiHijo) {

                            String prefijo = "chat.mcasocial.familiar_adulto.regalo.";

                            if (this.itemName.equals(objetoObsesion) && regalosDadosHoy == 0) {
                                llaveDialogo = prefijo + "favorito";
                            } else if (regalosDadosHoy >= 2) {

                                if (puntosGanados > 0) {
                                    llaveDialogo = prefijo + "sospecha_bueno";
                                } else if (puntosGanados < 0) {
                                    llaveDialogo = prefijo + "sospecha_malo";
                                } else {
                                    llaveDialogo = prefijo + "sospecha_normal";
                                }
                            } else if (puntosGanados > 0) {
                                llaveDialogo = prefijo + "bien";
                            } else {
                                llaveDialogo = prefijo + "mal";
                            }
                        }

                        if (finalEsUnBebe) {
                            llaveDialogo = "chat.mcasocial.bebe.regalo";
                        }

                        if (entity instanceof VillagerEntityMCA mcaVillager) {
                            try {
                                var brain = mcaVillager.getVillagerBrain();
                                var memories = brain.getMemoriesForPlayer(player);
                                memories.setInteractionFatigue(memories.getInteractionFatigue() + 1);

                                int romanceActual = data.getRomance();
                                int corazonesNuevos = romanceActual + puntosGanados;

                                memories.setHearts(corazonesNuevos);
                                data.setRomance(corazonesNuevos);
                            } catch (Exception e) {}
                        }

                        itemRegalo.shrink(1);
                        data.setLastActionTime("regalar_dia", tiempoActual);
                        data.setActionUseCount("contador_regalos", regalosDadosHoy + 1);

                        String colorSuma = puntosGanados > 0 ? "§d(+" + puntosGanados + " ❤)" : (puntosGanados == 0 ? "§e(+0)" : "§c(" + puntosGanados + " ❤)");
                        player.sendSystemMessage(Component.literal("§d<" + nombreNpc + "> §f").append(Component.translatable(llaveDialogo)).append(Component.literal(" " + colorSuma)));

                        net.minecraft.sounds.SoundEvent sonido = puntosGanados >= 10 ? net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP : net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP;
                        player.level().playSound(null, player.blockPosition(), sonido, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, puntosGanados > 0 ? 1.2F : 0.6F);
                    });
                }
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
