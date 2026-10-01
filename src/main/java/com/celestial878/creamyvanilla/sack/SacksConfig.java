package com.celestial878.creamyvanilla.sack;

import net.neoforged.neoforge.common.ModConfigSpec;

public class SacksConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SACK_PENALTY;
    public static final ModConfigSpec.IntValue SACK_INCREMENT;
    public static final ModConfigSpec.IntValue SACK_SLOTS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("sack");
        SACK_PENALTY = builder
                .comment("Penalize the player with the overencumbered effect when carrying too many sacks")
                .define("sack_penalty", true);
        SACK_INCREMENT = builder
                .comment("Maximum number of sacks after which the overencumbered effect will be applied. "
                        + "Each multiple of this number will increase the effect strength by one")
                .defineInRange("sack_increment", 2, 1, 50);
        SACK_SLOTS = builder
                .comment("How many slots should a sack have")
                .defineInRange("slots", 9, 1, SackBlockEntity.MAX_SIZE);
        builder.pop();
        SPEC = builder.build();
    }
}
