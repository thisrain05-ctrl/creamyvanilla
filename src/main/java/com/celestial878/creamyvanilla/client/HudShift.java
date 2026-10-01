package com.celestial878.creamyvanilla.client;

import java.util.HashSet;
import java.util.Set;

import com.celestial878.creamyvanilla.CreamyVanilla;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/** Client only: nudges the HUD layers of other mods down so they line up with the vanilla bars. */
@EventBusSubscriber(modid = CreamyVanilla.MODID, value = Dist.CLIENT)
public final class HudShift {
    private static final double SHIFT = 7.0; // same value No XP uses

    private static final Set<ResourceLocation> LAYERS = Set.of(
        ResourceLocation.fromNamespaceAndPath("appleskin", "health_restored"),
        ResourceLocation.fromNamespaceAndPath("appleskin", "hunger_restored"),
        ResourceLocation.fromNamespaceAndPath("appleskin", "saturation_level"),
        ResourceLocation.fromNamespaceAndPath("appleskin", "exhaustion_level"),
        ResourceLocation.fromNamespaceAndPath("toughnessbar", "armor_toughness")
        // add any other mod's layer IDs here
    );

    private static final Set<ResourceLocation> pushed = new HashSet<>();

    private HudShift() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPre(RenderGuiLayerEvent.Pre event) {
        ResourceLocation name = event.getName();
        if (LAYERS.contains(name) && !event.isCanceled()) {
            var pose = event.getGuiGraphics().pose();
            pose.pushPose();
            pose.translate(0.0, SHIFT, 0.0);
            pushed.add(name);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPost(RenderGuiLayerEvent.Post event) {
        if (pushed.remove(event.getName())) {
            event.getGuiGraphics().pose().popPose();
        }
    }
}
