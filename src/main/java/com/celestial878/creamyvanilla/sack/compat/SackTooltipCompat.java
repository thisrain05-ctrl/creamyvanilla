package com.celestial878.creamyvanilla.sack.compat;

import com.misterpemodder.shulkerboxtooltip.api.neoforge.ShulkerBoxTooltipPlugin;

import net.neoforged.fml.ModContainer;

/** Only ever loaded when ShulkerBoxTooltip is present (see CreamyVanilla). */
public final class SackTooltipCompat {
    private SackTooltipCompat() {
    }

    public static void register(ModContainer container) {
        // Pass the plugin itself (not a lambda) so this doesn't clash with the Supplier overload.
        container.registerExtensionPoint(ShulkerBoxTooltipPlugin.class,
                new ShulkerBoxTooltipPlugin(SackTooltipApi::new));
    }
}
