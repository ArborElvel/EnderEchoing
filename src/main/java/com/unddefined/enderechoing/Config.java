package com.unddefined.enderechoing;

import net.neoforged.neoforge.common.ModConfigSpec;

// Common gameplay configuration for Ender Echoing.
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue ENDER_ECHOING_CORE_COOLDOWN = BUILDER
            .comment("Cooldown of the Ender Echoing Core, in seconds.")
            .defineInRange("EECore_Cooldown", 15, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue EECORE_TP_DISTANCE = BUILDER
            .comment("Distance interval used by the Ender Echoing Core. Each interval consumes one Ender Echo Pearl.")
            .defineInRange("EECore_TP_Distance", 256, 64, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue EECrystal_TP_DISTANCE = BUILDER
            .comment("Maximum teleport distance of the Ender Echo Crystal, in blocks.")
            .defineInRange("EECrystal_TP_Distance", 96, 16, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue EECrystal_HEAL_DISTANCE = BUILDER
            .comment("Maximum distance at which an Ender Echo Crystal or End Crystal can heal the player, in blocks.")
            .defineInRange("EECrystal_heal_Distance", 16, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue EECrystal_HEAL_AMOUNT = BUILDER
            .comment("Health restored by an Ender Echo Crystal.")
            .defineInRange("EECrystal_Heal_Amount", 2, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue EECrystal_HEAL_XP_COST = BUILDER
            .comment("Experience points consumed by each Ender Echo Crystal heal.")
            .defineInRange("EECrystal_Heal_XPCost", 100, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue EECrystal_HEAL_INTERVAL = BUILDER
            .comment("Interval between Ender Echo Crystal heals, in seconds.")
            .defineInRange("EECrystal_Heal_Interval", 8, 1, Integer.MAX_VALUE);


    public static final ModConfigSpec.IntValue EndCrystal_HEAL_AMOUNT = BUILDER
            .comment("Health restored by an End Crystal.")
            .defineInRange("End_Crystal_Heal_Amount", 1, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue EndCrystal_HEAL_XP_COST = BUILDER
            .comment("Experience points consumed by each End Crystal heal.")
            .defineInRange("End_Crystal_Heal_XPCost", 100, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue EndCrystal_HEAL_INTERVAL = BUILDER
            .comment("Interval between End Crystal heals, in seconds.")
            .defineInRange("End_Crystal_Heal_Interval", 8, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SCULK_VEIL_DARKNESS_DURATION = BUILDER
            .comment("Duration of the Darkness effect applied by Sculk Veil, in seconds.")
            .defineInRange("sculk_veil_darkness_duration", 20, 20, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SCULK_VEIL_GLOWING_DURATION = BUILDER
            .comment("Duration of the Glowing effect applied by Sculk Veil, in seconds.")
            .defineInRange("sculk_veil_glowing_duration", 25, 20, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ECHO_DRUSE_MAX_GROWTH_VALUE = BUILDER
            .comment("Maximum growth value of an Echo Druse.")
            .defineInRange("echo_druse_max_growth_value", 40000, 4, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SCULK_SHRIEKER_CAN_SUMMON_CHANCE = BUILDER
            .comment("1 in N chance for a Sculk Shrieker to gain CAN_SUMMON when a nearby entity dies on a Sculk Catalyst.")
            .defineInRange("sculk_shrieker_can_summon_chance", 7, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue EchoSoundingDistance = BUILDER
            .comment("Maximum distance at which a resonator can detect another resonator, in blocks.")
            .defineInRange("Echo_Sounding_Distance", 2048, 1, Integer.MAX_VALUE);


    static final ModConfigSpec SPEC = BUILDER.build();
}
