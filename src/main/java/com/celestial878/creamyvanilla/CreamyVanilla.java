package com.celestial878.creamyvanilla;

import com.celestial878.creamyvanilla.attribute.FireResistance;
import com.celestial878.creamyvanilla.attribute.LootingAttribute;
import com.celestial878.creamyvanilla.keephotbar.CuriosCompat;
import com.celestial878.creamyvanilla.keephotbar.KeepHotbarEvents;
import com.celestial878.creamyvanilla.sack.SackRegistry;
import com.celestial878.creamyvanilla.sack.SacksConfig;
import com.celestial878.creamyvanilla.sack.compat.SackTooltipCompat;

import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/**
 * Creamy Vanilla - created and owned by Celestial878.
 * <p>
 * One mod bundling: Sacks, the Harvesting enchantment, the Looting attribute, the Fire Resistance attribute
 * (needs Puffish Attributes), Keep Hotbar Gear and HUD Shift (client only).
 * <p>
 * Credits: the sack is a fork of the sack from Supplementaries (code and art), and the right-click harvesting
 * action is based on code from Harvest with Ease. See CREDITS.md.
 */
@Mod(CreamyVanilla.MODID)
public class CreamyVanilla {
    public static final String MODID = "creamyvanilla";

    public CreamyVanilla(IEventBus modEventBus, ModContainer modContainer) {
        // --- Sacks (fork of Supplementaries' sack) ---
        SackRegistry.SOUNDS.register(modEventBus);
        SackRegistry.BLOCKS.register(modEventBus);
        SackRegistry.ITEMS.register(modEventBus);
        SackRegistry.BLOCK_ENTITIES.register(modEventBus);
        SackRegistry.MENUS.register(modEventBus);
        SackRegistry.EFFECTS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, SacksConfig.SPEC);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerCapabilities);
        // Only touch the compat class when the other mod is present, so it stays optional.
        if (ModList.get().isLoaded("shulkerboxtooltip")) {
            SackTooltipCompat.register(modContainer);
        }

        // --- Looting attribute ---
        LootingAttribute.register(modEventBus);

        // --- Fire Resistance attribute: built on Puffish Attributes, so only enabled when that mod is present ---
        if (ModList.get().isLoaded("puffish_attributes")) {
            FireResistance.register(modEventBus);
        }

        // --- Keep Hotbar Gear: on death keep hotbar, armor, offhand (and Curios when present) ---
        NeoForge.EVENT_BUS.register(new KeepHotbarEvents());
        // Only touch the compat class when Curios is present, so it stays optional.
        if (ModList.get().isLoaded("curios")) {
            CuriosCompat.init();
        }

        // The Harvesting enchantment (HarvestingHandler) and HUD Shift (client/HudShift) register themselves
        // through @EventBusSubscriber, and the enchantment itself is data-driven.
    }

    // Lets pipes, conduits and other mods insert into and extract from placed sacks.
    // InvWrapper honours canPlaceItem, so sacks and shulker boxes still can't be nested.
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SackRegistry.SACK_BE.get(),
                (sack, side) -> new InvWrapper(sack));
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Dispensers place sacks the same way they place shulker boxes.
        event.enqueueWork(() ->
                DispenserBlock.registerBehavior(SackRegistry.SACK_ITEM.get(), new ShulkerBoxDispenseBehavior()));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(SackRegistry.SACK_ITEM.get());
        }
    }
}
