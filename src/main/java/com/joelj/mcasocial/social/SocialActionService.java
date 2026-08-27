package com.joelj.mcasocial.social;

import forge.net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class SocialActionService {
    private static final Set<String> VALID_ACTIONS = Set.of(
            "saludar", "bromear", "elogiar", "insultar", "coquetear",
            "amistad_saludar", "preguntar_dia", "chismear", "contar_anecdota", "dar_apoyo",
            "recordar_votos", "hablar_hogar", "planear_juntos"
    );

    private SocialActionService() {}

    public static boolean isValidAction(String action) {
        return VALID_ACTIONS.contains(action)
                || RelationshipActionService.isRelationshipAction(action)
                || HumorActionService.isHumorAction(action);
    }

    public static RoleInfo inspectRole(Entity entity, boolean isFamily) {
        return inspectRole(entity, null, isFamily);
    }

    public static RoleInfo inspectRole(Entity entity, @Nullable Player player, boolean isFamily) {
        boolean isBaby = false;
        boolean isMinor = false;
        boolean family = isFamily;
        boolean spouse = false;
        SocialAgeStage ageStage = SocialAgeStage.ADULT;

        if (entity instanceof net.minecraft.world.entity.AgeableMob ageable) {
            isBaby = ageable.isBaby();
            if (isBaby) ageStage = SocialAgeStage.BABY;
        }
        if (entity instanceof VillagerEntityMCA mcaVillager) {
            try {
                java.lang.reflect.Method getAgeStateMethod = mcaVillager.getClass().getMethod("getAgeState");
                String ageState = getAgeStateMethod.invoke(mcaVillager).toString();
                ageStage = SocialAgeStage.fromMcaName(ageState);
                isBaby = ageStage == SocialAgeStage.BABY;
                isMinor = ageStage == SocialAgeStage.CHILD || ageStage == SocialAgeStage.TEEN;
            } catch (Exception ignored) {}

            if (player != null) {
                try {
                    var relationships = mcaVillager.getRelationships();
                    if (relationships.isMarriedTo(player.getUUID())) {
                        spouse = true;
                    }

                    var familyEntry = relationships.getFamilyEntry();
                    if (familyEntry != null) {
                        if (familyEntry.isParent(player.getUUID())) {
                            family = true;
                        }
                        if (familyEntry.partner() != null && familyEntry.partner().equals(player.getUUID())
                                && familyEntry.getRelationshipState().isMarried()) {
                            spouse = true;
                        }
                    }
                } catch (Exception ignored) {}

                try {
                    var memories = mcaVillager.getVillagerBrain().getMemoriesForPlayer(player);
                    String dialogueType = String.valueOf(memories.getDialogueType()).toUpperCase(Locale.ROOT);
                    if (dialogueType.equals("SPOUSE")) {
                        spouse = true;
                    } else if (isParentDialogue(dialogueType)) {
                        family = true;
                    }
                } catch (Exception ignored) {}

                try {
                    net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
                    mcaVillager.saveWithoutId(nbt);
                    String playerUuid = player.getUUID().toString();

                    family = family || playerUuid.equals(nbt.getString("fatherUUID"))
                            || playerUuid.equals(nbt.getString("motherUUID"))
                            || playerUuid.equals(nbt.getString("father"))
                            || playerUuid.equals(nbt.getString("mother"))
                            || (nbt.contains("parents") && nbt.get("parents").toString().contains(playerUuid));
                } catch (Exception ignored) {}
            }
        }

        return new RoleInfo(entity.getName().getString(), isBaby, isMinor, family && !spouse, spouse, ageStage);
    }

    private static boolean isParentDialogue(String dialogueType) {
        return dialogueType != null && (dialogueType.endsWith("P") || dialogueType.contains("PARENT"));
    }

    public static void perform(ServerPlayer player, @Nullable Entity entity, ISocialData data,
                               String action, RoleInfo role, boolean remoteMessage) {
        perform(player, entity, data, action, role, remoteMessage ? InteractionChannel.PHONE : InteractionChannel.FACE_TO_FACE);
    }

    public static void perform(ServerPlayer player, @Nullable Entity entity, ISocialData data,
                               String action, RoleInfo role, InteractionChannel channel) {
        if (!isValidAction(action)) return;
        if (RelationshipActionService.isRelationshipAction(action)) {
            RelationshipActionService.perform(player, entity, data, action, role, channel);
            return;
        }
        if (HumorActionService.isHumorAction(action)) {
            HumorActionService.perform(player, entity, data, action, role, channel);
            return;
        }

        String nombre = role.name();
        int puntosGanados = 0;
        String llaveDialogo = "";
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double dado = random.nextDouble();

        String p = normalizePersonality(data.getPersonality());

        long tiempoActual = System.currentTimeMillis();
        long tiempoTranscurrido = tiempoActual - data.getLastActionTime(action);

        if (tiempoActual < data.getLockoutTime()) {
            MutableComponent mensajeChat = Component.literal(prefix(nombre, channel))
                    .append(Component.translatable(role.isBaby() ? "chat.mcasocial.bebe.insulto" : "chat.mcasocial.global.bloqueado"))
                    .append(Component.literal(" \u00a7e(+0)"));
            player.sendSystemMessage(mensajeChat);
            player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5F, 1.0F);
            return;
        }

        long ultimoSpam = data.getLastActionTime("GLOBAL_SPAM_TIMER");
        if (tiempoActual - ultimoSpam > 2 * 60 * 1000) {
            data.setSpamCount(0);
        }

        boolean esRomance = action.equals("coquetear");
        boolean esAmistadExtra = isFriendshipAction(action);
        int nivelRelacion = getRelationLevel(esRomance ? data.getRomance() : data.getAffinity());
        long tiempoEsperaRequerido = cooldownFor(action, p, nivelRelacion, role.isSpouse());

        double probabilidadIgnorar = switch (nivelRelacion) {
            case 1 -> 0.10;
            case 2 -> 0.35;
            case 3 -> 0.65;
            default -> 0.95;
        };
        if (role.isSpouse()) {
            probabilidadIgnorar = Math.min(0.98, probabilidadIgnorar + 0.15);
        }

        int limiteNeutralBase = switch (nivelRelacion) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 5;
            default -> 8;
        };
        int limiteHartoBase = switch (nivelRelacion) {
            case 1 -> 4;
            case 2 -> 6;
            case 3 -> 9;
            default -> 14;
        };
        int limiteNeutral = adjustNeutralLimit(limiteNeutralBase, p, nivelRelacion, action, role.isSpouse());
        int limiteHarto = adjustHartoLimit(limiteHartoBase, p, nivelRelacion, action, limiteNeutral, role.isSpouse());

        boolean esSpam = tiempoTranscurrido < tiempoEsperaRequerido;
        boolean bypassExitoso = false;

        if (!esSpam) {
            data.setActionUseCount(action, 0);
        }

        if (esSpam && random.nextDouble() < probabilidadIgnorar) bypassExitoso = true;

        if (esSpam) {
            data.setSpamCount(data.getSpamCount() + 1);
            data.setLastActionTime("GLOBAL_SPAM_TIMER", tiempoActual);
        } else {
            data.setSpamCount(0);
        }

        int insistencia = data.getSpamCount();
        boolean ejecutarAccionNormal = false;
        String palabraClave = dialogueWord(action);

        if (esSpam && insistencia >= limiteHarto) {
            data.setLockoutTime(tiempoActual + (5 * 60 * 1000));
            puntosGanados = -1;
            llaveDialogo = "chat.mcasocial.global.harto";
        } else if (esSpam && insistencia >= limiteNeutral) {
            puntosGanados = 0;
            llaveDialogo = "chat.mcasocial.global.neutral." + random.nextInt(1, 3);
        } else if (esSpam && !bypassExitoso) {
            if (nivelRelacion >= 3 && !action.equals("insultar")) {
                if (random.nextDouble() < 0.05) {
                    puntosGanados = -1;
                    llaveDialogo = "chat.mcasocial." + palabraClave + ".spam.malhumor";
                } else {
                    puntosGanados = 0;
                    llaveDialogo = "chat.mcasocial." + palabraClave + ".spam.amable";
                }
            } else {
                puntosGanados = -1;
                llaveDialogo = "chat.mcasocial." + palabraClave + ".spam." + random.nextInt(1, 4);
            }
        } else {
            ejecutarAccionNormal = true;
        }

        if (ejecutarAccionNormal) {
            int usosActuales = data.getActionUseCount(action);
            int limiteUsosNarrativo = narrativeLimit(action, role.isSpouse());

            if (usosActuales >= limiteUsosNarrativo) {
                puntosGanados = 0;
                llaveDialogo = "chat.mcasocial." + palabraClave + ".limite.lvl" + nivelRelacion;
            } else {
                data.setActionUseCount(action, usosActuales + 1);
                data.setLastActionTime(action, tiempoActual);

                ActionOutcome outcome = calculateOutcome(action, p, dado, random, data, role, channel, player);
                puntosGanados = outcome.points();
                llaveDialogo = outcome.dialogueKey();

                if (!esAmistadExtra) {
                    llaveDialogo = applyFamilyOverrides(action, palabraClave, role, esSpam, puntosGanados, llaveDialogo, player);
                }
            }
        }

        if (role.isBaby()) {
            if (puntosGanados >= 0) {
                if (action.equals("saludar") || action.equals("amistad_saludar")) llaveDialogo = "chat.mcasocial.bebe.saludo";
                else if (action.equals("bromear") || action.equals("contar_anecdota")) llaveDialogo = "chat.mcasocial.bebe.broma";
                else llaveDialogo = "chat.mcasocial.bebe.elogio";
            } else {
                llaveDialogo = "chat.mcasocial.bebe.insulto";
            }
        }

        applyPoints(player, entity, data, action, puntosGanados, esRomance, ejecutarAccionNormal);
        sendResult(player, nombre, llaveDialogo, puntosGanados, esRomance || action.equals("insultar"), channel);
    }

    private static ActionOutcome calculateOutcome(String action, String p, double dado, ThreadLocalRandom random,
                                                  ISocialData data, RoleInfo role, InteractionChannel channel, ServerPlayer player) {
        return switch (action) {
            case "saludar" -> {
                int varSaludo = random.nextInt(1, 6);
                yield new ActionOutcome(3, "chat.mcasocial.saludo." + p + "." + varSaludo);
            }
            case "bromear" -> jokeOutcome(p, dado, random);
            case "elogiar" -> praiseOutcome(p, dado, random);
            case "insultar" -> insultOutcome(p, dado, random);
            case "coquetear" -> flirtOutcome(p, dado, random, data);
            case "amistad_saludar" -> friendshipOutcome("saludo", p, random, data, role, channel, 2, player);
            case "preguntar_dia" -> friendshipOutcome("preguntar_dia", p, random, data, role, channel,
                    p.equals("timido") || p.equals("leal") || p.equals("romantico") ? 5 : 4, player);
            case "chismear" -> friendshipOutcome("chismear", p, random, data, role, channel,
                    p.equals("chismoso") ? 8 : (p.equals("leal") || p.equals("orgulloso") ? -1 : 4), player);
            case "contar_anecdota" -> friendshipOutcome("contar_anecdota", p, random, data, role, channel,
                    p.equals("extrovertido") || p.equals("sarcastico") ? 6 : 4, player);
            case "dar_apoyo" -> friendshipOutcome("dar_apoyo", p, random, data, role, channel,
                    p.equals("timido") || p.equals("leal") || p.equals("romantico") ? 7 : 4, player);
            case "recordar_votos" -> spouseOutcome("recordar_votos", role, channel, 8);
            case "hablar_hogar" -> spouseOutcome("hablar_hogar", role, channel, 7);
            case "planear_juntos" -> spouseOutcome("planear_juntos", role, channel, 8);
            default -> new ActionOutcome(0, "chat.mcasocial.global.neutral.1");
        };
    }

    private static ActionOutcome jokeOutcome(String p, double dado, ThreadLocalRandom random) {
        double probBroma = 0.60;
        int ptsBienBroma = 6;
        int ptsMalBroma = -1;
        if (p.equals("extrovertido")) { probBroma = 0.85; ptsBienBroma = 9; ptsMalBroma = 0; }
        else if (p.equals("orgulloso")) { probBroma = 0.30; ptsBienBroma = 3; ptsMalBroma = -2; }
        else if (p.equals("chismoso")) { probBroma = 0.70; ptsBienBroma = 6; ptsMalBroma = 0; }
        else if (p.equals("sarcastico")) { probBroma = 0.40; ptsBienBroma = 6; ptsMalBroma = -3; }

        if (dado < probBroma) return new ActionOutcome(ptsBienBroma, "chat.mcasocial.broma." + p + ".bien." + random.nextInt(1, 4));
        return new ActionOutcome(ptsMalBroma, "chat.mcasocial.broma." + p + ".mal." + random.nextInt(1, 3));
    }

    private static ActionOutcome praiseOutcome(String p, double dado, ThreadLocalRandom random) {
        double probElogio = 0.80;
        int ptsBienElogio = 9;
        int ptsMalElogio = -1;
        if (p.equals("orgulloso")) { probElogio = 0.60; ptsBienElogio = 12; ptsMalElogio = -2; }
        else if (p.equals("timido")) { probElogio = 0.50; ptsBienElogio = 9; ptsMalElogio = -1; }
        else if (p.equals("sarcastico")) { probElogio = 0.40; ptsBienElogio = 6; ptsMalElogio = -3; }
        else if (p.equals("romantico")) { probElogio = 0.90; ptsBienElogio = 12; ptsMalElogio = 0; }

        if (dado < probElogio) return new ActionOutcome(ptsBienElogio, "chat.mcasocial.elogio." + p + ".bien." + random.nextInt(1, 4));
        return new ActionOutcome(ptsMalElogio, "chat.mcasocial.elogio." + p + ".mal." + random.nextInt(1, 3));
    }

    private static ActionOutcome insultOutcome(String p, double dado, ThreadLocalRandom random) {
        double probInsulto = 0.70;
        int ptsBienInsulto = -5;
        int ptsMalInsulto = -2;
        if (p.equals("leal")) { probInsulto = 1.0; ptsBienInsulto = -4; ptsMalInsulto = -4; }
        else if (p.equals("orgulloso")) { probInsulto = 0.50; ptsBienInsulto = -6; ptsMalInsulto = -1; }
        else if (p.equals("sarcastico")) { probInsulto = 0.30; ptsBienInsulto = -3; ptsMalInsulto = 0; }

        if (dado < probInsulto) return new ActionOutcome(ptsBienInsulto, "chat.mcasocial.insulto." + p + ".bien." + random.nextInt(1, 4));
        return new ActionOutcome(ptsMalInsulto, "chat.mcasocial.insulto." + p + ".mal." + random.nextInt(1, 3));
    }

    private static ActionOutcome flirtOutcome(String p, double dado, ThreadLocalRandom random, ISocialData data) {
        if (data.getAffinity() < 10) return new ActionOutcome(-5, "chat.mcasocial.ligar.creepy");

        double probLigue = 0.60;
        if (p.equals("romantico")) probLigue = 0.85;
        else if (p.equals("orgulloso")) probLigue = 0.40;
        else if (p.equals("timido")) probLigue = 0.50;

        if (dado < probLigue) {
            if (p.equals("normal")) return new ActionOutcome(12, "chat.mcasocial.ligar.bien." + random.nextInt(1, 7));
            return new ActionOutcome(12, "chat.mcasocial.ligar." + p + ".bien." + random.nextInt(1, 4));
        }

        if (p.equals("normal")) return new ActionOutcome(-2, "chat.mcasocial.ligar.mal." + random.nextInt(1, 3));
        return new ActionOutcome(-2, "chat.mcasocial.ligar." + p + ".mal." + random.nextInt(1, 3));
    }

    private static ActionOutcome friendshipOutcome(String base, String personality, ThreadLocalRandom random,
                                                   ISocialData data, RoleInfo role, InteractionChannel channel,
                                                   int points, ServerPlayer player) {
        String roleKey = friendshipRoleKey(base, data, role, channel, player);
        if (roleKey != null) return new ActionOutcome(adjustRoleFriendshipPoints(base, points, role), roleKey);
        if (channel == InteractionChannel.PHONE || channel == InteractionChannel.CRYSTAL) {
            return new ActionOutcome(points, "chat.mcasocial.amistad." + base + "." + personality + "." + random.nextInt(1, 6));
        }
        return new ActionOutcome(points, "chat.mcasocial.amistad." + base + "." + channel.keySegment() + "." + personality + "." + random.nextInt(1, 6));
    }

    private static int adjustRoleFriendshipPoints(String base, int points, RoleInfo role) {
        if (base.equals("chismear") && role.isFamily() && !role.isSpouse()) {
            return Math.max(points, 4);
        }
        return points;
    }

    private static ActionOutcome spouseOutcome(String base, RoleInfo role, InteractionChannel channel, int points) {
        if (!role.isSpouse()) return new ActionOutcome(0, "chat.mcasocial.amistad.matrimonio.solo_conyuge");
        return new ActionOutcome(points, "chat.mcasocial.amistad." + base + "." + channel.keySegment() + ".conyuge");
    }

    @Nullable
    private static String friendshipRoleKey(String base, ISocialData data, RoleInfo role, InteractionChannel channel, ServerPlayer player) {
        if (role.isSpouse()) return "chat.mcasocial.amistad." + base + "." + channel.keySegment() + ".conyuge";
        if (role.isMinor() && role.isFamily()) return "chat.mcasocial.amistad." + base + "." + channel.keySegment() + ".hijo" + detectPlayerParentSuffix(player);
        if (role.isMinor()) return "chat.mcasocial.amistad." + base + "." + channel.keySegment() + ".menor";
        if (role.isFamily()) return "chat.mcasocial.amistad." + base + "." + channel.keySegment() + ".hijo" + detectPlayerParentSuffix(player);
        if (data.getRomance() >= 75) return "chat.mcasocial.amistad." + base + ".pareja";
        if (data.getRomance() >= 50) return "chat.mcasocial.amistad." + base + ".saliente.3";
        if (data.getRomance() >= 25) return "chat.mcasocial.amistad." + base + ".saliente.2";
        if (data.getRomance() >= 10) return "chat.mcasocial.amistad." + base + ".saliente.1";
        return null;
    }

    private static String applyFamilyOverrides(String action, String palabraClave, RoleInfo role, boolean esSpam,
                                               int puntosGanados, String llaveDialogo, ServerPlayer player) {
        String suffixParent = detectPlayerParentSuffix(player);

        if (role.isMinor() && !role.isBaby()) {
            String prefijo = role.isFamily() ? "chat.mcasocial.hijo." : "chat.mcasocial.menor.";
            if (action.equals("coquetear")) return prefijo + "ligar";
            if (llaveDialogo.contains(".limite.")) return prefijo + palabraClave + ".limite";
            if (esSpam) return prefijo + "spam";
            if (action.equals("saludar")) return prefijo + "saludo" + (role.isFamily() ? suffixParent : "");
            return prefijo + palabraClave + (puntosGanados > 0 ? ".bien" : ".mal");
        }

        if (!role.isMinor() && !role.isBaby() && role.isFamily()) {
            String prefijo = "chat.mcasocial.familiar_adulto.";
            if (action.equals("coquetear")) return prefijo + "ligar";
            if (llaveDialogo.contains(".limite.")) return prefijo + palabraClave + ".limite";
            if (esSpam) return prefijo + "spam";
            if (action.equals("saludar")) return prefijo + "saludo" + suffixParent;
            return prefijo + palabraClave + (puntosGanados > 0 ? ".bien" : ".mal");
        }

        return llaveDialogo;
    }

    private static void applyPoints(ServerPlayer player, @Nullable Entity entity, ISocialData data, String action,
                                    int puntosGanados, boolean esRomance, boolean ejecutarAccionNormal) {
        if (esRomance) {
            updateMcaHearts(player, entity, data, puntosGanados);
            return;
        }

        data.setAffinity(data.getAffinity() + puntosGanados);
        if (action.equals("insultar") && ejecutarAccionNormal) {
            updateMcaHearts(player, entity, data, puntosGanados);
        }
    }

    private static void updateMcaHearts(ServerPlayer player, @Nullable Entity entity, ISocialData data, int puntosGanados) {
        int romanceActual = data.getRomance();
        int nuevosCorazones = romanceActual + puntosGanados;
        data.setRomance(nuevosCorazones);

        if (entity instanceof VillagerEntityMCA mcaVillager) {
            try {
                var brain = mcaVillager.getVillagerBrain();
                var memories = brain.getMemoriesForPlayer(player);
                memories.setHearts(nuevosCorazones);
            } catch (Exception ignored) {}
        }
    }

    private static void sendResult(ServerPlayer player, String nombre, String key, int points,
                                   boolean romanceStyle, InteractionChannel channel) {
        float tono = points > 0 ? 1.5F : (points == 0 ? 0.9F : 0.5F);
        String colorSuma;
        if (romanceStyle) {
            colorSuma = points > 0 ? "\u00a7d(+" + points + " \u2665)" : (points == 0 ? "\u00a7e(+0)" : "\u00a7c(" + points + " \u2665)");
        } else {
            colorSuma = points > 0 ? "\u00a7a(+" + points + ")" : (points == 0 ? "\u00a7e(+0)" : "\u00a7c(" + points + ")");
        }

        MutableComponent mensajeChat = Component.literal(prefix(nombre, channel))
                .append(Component.translatable(key))
                .append(Component.literal(" " + colorSuma));

        player.sendSystemMessage(mensajeChat);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, tono);
    }

    private static String prefix(String name, InteractionChannel channel) {
        return switch (channel) {
            case PHONE -> "\u00a79[SMS " + name + "] \u00a7f";
            case LETTER -> "\u00a76[Carta " + name + "] \u00a7f";
            case CRYSTAL -> "\u00a7b[Crystal " + name + "] \u00a7f";
            case FACE_TO_FACE -> "\u00a7d<" + name + "> \u00a7f";
        };
    }

    private static int getRelationLevel(int value) {
        if (value >= 75) return 4;
        if (value >= 50) return 3;
        if (value >= 25) return 2;
        return 1;
    }

    private static long cooldownFor(String action, String personality, int relationLevel, boolean spouse) {
        long base = switch (action) {
            case "saludar" -> 7 * 60 * 1000L;
            case "bromear" -> 2 * 60 * 1000L;
            case "elogiar" -> 5 * 60 * 1000L;
            case "insultar" -> 20 * 1000L;
            case "coquetear" -> 5 * 60 * 1000L;
            case "amistad_saludar" -> 5 * 60 * 1000L;
            case "preguntar_dia" -> 4 * 60 * 1000L;
            case "chismear", "contar_anecdota" -> 3 * 60 * 1000L;
            case "dar_apoyo" -> 6 * 60 * 1000L;
            case "recordar_votos", "hablar_hogar", "planear_juntos" -> 3 * 60 * 1000L;
            default -> 60 * 1000L;
        };
        double multiplier = relationCooldownMultiplier(relationLevel) * personalityCooldownMultiplier(action, personality);
        if (spouse) multiplier *= 0.70;
        return Math.max(10_000L, Math.round(base * multiplier));
    }

    private static double relationCooldownMultiplier(int relationLevel) {
        return switch (relationLevel) {
            case 1 -> 1.15;
            case 2 -> 1.0;
            case 3 -> 0.85;
            default -> 0.70;
        };
    }

    private static double personalityCooldownMultiplier(String action, String personality) {
        return switch (personality) {
            case "extrovertido" -> 0.65;
            case "timido" -> action.equals("insultar") ? 1.10 : 1.35;
            case "ambicioso" -> action.equals("preguntar_dia") || action.equals("chismear") ? 0.95 : 1.05;
            case "chismoso" -> action.equals("chismear") || action.equals("preguntar_dia") || action.equals("amistad_saludar") ? 0.70 : 0.90;
            case "orgulloso" -> action.equals("elogiar") ? 0.95 : 1.15;
            case "leal" -> 0.90;
            case "sarcastico" -> action.equals("bromear") || action.equals("contar_anecdota") ? 0.90 : 1.05;
            case "romantico" -> action.equals("coquetear") || action.equals("dar_apoyo") || action.equals("preguntar_dia") ? 0.85 : 0.95;
            default -> 1.0;
        };
    }

    private static int adjustNeutralLimit(int base, String personality, int relationLevel, String action, boolean spouse) {
        int spouseBonus = spouse ? 3 : 0;
        return Math.max(1, base + fatigueBonus(personality, relationLevel, action, false) + spouseBonus);
    }

    private static int adjustHartoLimit(int base, String personality, int relationLevel, String action, int neutralLimit, boolean spouse) {
        int spouseBonus = spouse ? 5 : 0;
        return Math.max(neutralLimit + 2, base + fatigueBonus(personality, relationLevel, action, true) + spouseBonus);
    }

    private static int fatigueBonus(String personality, int relationLevel, String action, boolean hardLimit) {
        boolean close = relationLevel >= 3;
        return switch (personality) {
            case "extrovertido" -> hardLimit ? 4 : 2;
            case "timido" -> hardLimit ? (close ? -1 : -2) : (close ? 0 : -1);
            case "ambicioso" -> action.equals("preguntar_dia") || action.equals("chismear") ? 1 : 0;
            case "chismoso" -> {
                boolean favoriteTopic = action.equals("chismear") || action.equals("preguntar_dia") || action.equals("amistad_saludar");
                yield favoriteTopic ? (hardLimit ? 3 : 2) : (hardLimit ? 2 : 1);
            }
            case "orgulloso" -> hardLimit ? (close ? 1 : -1) : (close ? 0 : -1);
            case "leal" -> hardLimit ? 2 : 1;
            case "sarcastico" -> action.equals("bromear") || action.equals("contar_anecdota") ? 1 : 0;
            case "romantico" -> action.equals("coquetear") || action.equals("dar_apoyo") || action.equals("preguntar_dia")
                    ? (hardLimit ? 2 : 1)
                    : (hardLimit ? 1 : 0);
            default -> 0;
        };
    }

    private static int narrativeLimit(String action, boolean spouse) {
        int base = switch (action) {
            case "saludar", "amistad_saludar", "dar_apoyo" -> 1;
            case "recordar_votos", "hablar_hogar", "planear_juntos" -> 2;
            case "bromear", "elogiar", "preguntar_dia", "chismear", "contar_anecdota" -> 2;
            default -> 999;
        };
        return spouse && isFriendshipAction(action) ? base + 1 : base;
    }

    private static boolean isFriendshipAction(String action) {
        return action.equals("amistad_saludar") || action.equals("preguntar_dia") || action.equals("chismear")
                || action.equals("contar_anecdota") || action.equals("dar_apoyo") || action.equals("recordar_votos")
                || action.equals("hablar_hogar") || action.equals("planear_juntos");
    }

    private static String dialogueWord(String action) {
        return switch (action) {
            case "coquetear" -> "ligar";
            case "saludar" -> "saludo";
            case "bromear" -> "broma";
            case "elogiar" -> "elogio";
            case "insultar" -> "insulto";
            case "amistad_saludar" -> "amistad.saludo";
            case "preguntar_dia" -> "amistad.preguntar_dia";
            case "chismear" -> "amistad.chismear";
            case "contar_anecdota" -> "amistad.contar_anecdota";
            case "dar_apoyo" -> "amistad.dar_apoyo";
            case "recordar_votos", "hablar_hogar", "planear_juntos" -> "amistad.matrimonio";
            default -> action;
        };
    }

    private static String normalizePersonality(String personality) {
        if (personality == null) return "normal";
        String lower = personality.toLowerCase();
        if (lower.contains("extrovertido")) return "extrovertido";
        if (lower.contains("timido") || lower.contains("t\u00edmido")) return "timido";
        if (lower.contains("ambicioso")) return "ambicioso";
        if (lower.contains("chismoso")) return "chismoso";
        if (lower.contains("orgulloso") || lower.contains("serio")) return "orgulloso";
        if (lower.contains("leal") || lower.contains("pacifico") || lower.contains("pac\u00edfico")) return "leal";
        if (lower.contains("sarcastico") || lower.contains("sarc\u00e1stico")) return "sarcastico";
        if (lower.contains("romantico") || lower.contains("rom\u00e1ntico")) return "romantico";
        return "normal";
    }

    private static String detectPlayerParentSuffix(ServerPlayer player) {
        try {
            var playerData = forge.net.conczin.mca.server.world.data.PlayerSaveData.get(player);
            if (playerData != null && playerData.getGender().toString().equalsIgnoreCase("FEMALE")) return ".mama";
        } catch (Exception ignored) {}
        return ".papa";
    }

    private record ActionOutcome(int points, String dialogueKey) {}

    public enum InteractionChannel {
        FACE_TO_FACE("face"),
        PHONE("phone"),
        LETTER("letter"),
        CRYSTAL("phone");

        private final String keySegment;

        InteractionChannel(String keySegment) {
            this.keySegment = keySegment;
        }

        public String keySegment() {
            return this.keySegment;
        }

        public static InteractionChannel fromInterfaceType(int interfaceType) {
            return switch (SocialInterfaceType.normalize(interfaceType)) {
                case SocialInterfaceType.LETTER -> LETTER;
                case SocialInterfaceType.CRYSTAL -> CRYSTAL;
                default -> PHONE;
            };
        }
    }

    public record RoleInfo(String name, boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse,
                           SocialAgeStage ageStage) {
        public RoleInfo(String name, boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse) {
            this(name, isBaby, isMinor, isFamily, isSpouse, SocialAgeStage.fromFlags(isBaby, isMinor));
        }
    }
}
