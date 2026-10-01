package com.celestial878.creamyvanilla.attribute;

import com.celestial878.creamyvanilla.CreamyVanilla;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Adds a {@code creamyvanilla:looting} item attribute. Attribute modifiers for it on an item (while in the main
 * hand) grant that many extra levels of Looting.
 */
public final class LootingAttribute {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, CreamyVanilla.MODID);

    public static final DeferredHolder<Attribute, Attribute> LOOTING =
            ATTRIBUTES.register("looting", () ->
                    new RangedAttribute("attribute.creamyvanilla.looting", 0.0D, 0.0D, 10.0D).setSyncable(true));

    private LootingAttribute() {
    }

    public static void register(IEventBus modBus) {
        ATTRIBUTES.register(modBus);
        NeoForge.EVENT_BUS.addListener(LootingAttribute::onGetEnchantmentLevel);
    }

    private static void onGetEnchantmentLevel(GetEnchantmentLevelEvent event) {
        if (!event.isTargetting(Enchantments.LOOTING)) return;

        ItemStack stack = event.getStack();
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        if (modifiers.modifiers().isEmpty()) return;

        double total = 0.0D;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().is(LOOTING.getId()) && entry.slot().test(EquipmentSlot.MAINHAND)) {
                total += entry.modifier().amount();
            }
        }

        int bonus = (int) Math.round(total);
        if (bonus <= 0) return;

        event.getHolder(Enchantments.LOOTING).ifPresent(holder ->
                event.getEnchantments().set(holder, event.getEnchantments().getLevel(holder) + bonus));
    }
}
