package com.celestial878.creamyvanilla.sack.compat;

import com.misterpemodder.shulkerboxtooltip.api.PreviewContext;
import com.misterpemodder.shulkerboxtooltip.api.provider.BlockEntityPreviewProvider;
import com.misterpemodder.shulkerboxtooltip.api.renderer.PreviewRenderer;
import com.celestial878.creamyvanilla.sack.SackBlockEntity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Shows the sack's contents in the tooltip, sized to the configured slot count, on the sack GUI texture. */
public final class SackPreviewProvider extends BlockEntityPreviewProvider {
    @OnlyIn(Dist.CLIENT)
    private static PreviewRenderer renderer;

    public SackPreviewProvider() {
        super(SackBlockEntity.MAX_SIZE, true);
    }

    @Override
    public int getInventoryMaxSize(PreviewContext context) {
        return SackBlockEntity.getUnlockedSlots();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public PreviewRenderer getRenderer() {
        if (renderer == null) {
            renderer = new SackPreviewRenderer(SackBlockEntity::getUnlockedSlots);
        }
        return renderer;
    }
}
