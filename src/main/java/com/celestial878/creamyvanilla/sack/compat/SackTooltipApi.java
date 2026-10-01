package com.celestial878.creamyvanilla.sack.compat;

import com.misterpemodder.shulkerboxtooltip.api.ShulkerBoxTooltipApi;
import com.misterpemodder.shulkerboxtooltip.api.provider.PreviewProviderRegistry;
import com.celestial878.creamyvanilla.sack.SackRegistry;
import com.celestial878.creamyvanilla.CreamyVanilla;

import net.minecraft.resources.ResourceLocation;

public class SackTooltipApi implements ShulkerBoxTooltipApi {
    @Override
    public void registerProviders(PreviewProviderRegistry registry) {
        registry.register(ResourceLocation.fromNamespaceAndPath(CreamyVanilla.MODID, "sack"),
                new SackPreviewProvider(), SackRegistry.SACK_ITEM.get());
    }
}
