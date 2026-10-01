package com.celestial878.creamyvanilla.keephotbar;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;

public final class CuriosCompat {

    private CuriosCompat() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(CuriosCompat::onDropRules);
    }

    private static void onDropRules(DropRulesEvent event) {
        if (event.getEntity() instanceof Player) {
            event.addOverride(stack -> true, ICurio.DropRule.ALWAYS_KEEP);
        }
    }
}
