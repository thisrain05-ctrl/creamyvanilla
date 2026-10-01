package com.celestial878.creamyvanilla.attribute;

import com.celestial878.creamyvanilla.CreamyVanilla;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.puffish.attributesmod.api.DynamicEntityAttribute;
import net.puffish.attributesmod.api.DynamicModification;

/**
 * Adds a {@code creamyvanilla:fire_resistance} attribute that reduces fire damage.
 * Built on Puffish Attributes, so this class is only ever loaded when that mod is installed
 * (see {@link CreamyVanilla}).
 */
public final class FireResistance {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, CreamyVanilla.MODID);

    public static final DeferredHolder<Attribute, Attribute> FIRE_RESISTANCE =
            ATTRIBUTES.register("fire_resistance", () ->
                    DynamicEntityAttribute
                            .create(ResourceLocation.fromNamespaceAndPath(CreamyVanilla.MODID, "fire_resistance"))
                            .setSyncable(true));

    private FireResistance() {
    }

    public static void register(IEventBus modBus) {
        ATTRIBUTES.register(modBus);
        modBus.addListener(FireResistance::addToEntities);
        NeoForge.EVENT_BUS.addListener(FireResistance::onIncomingDamage);
    }

    private static void addToEntities(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> event.add(type, FIRE_RESISTANCE));
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_FIRE)) return;

        float reduced = DynamicModification.create()
                .withNegative(FIRE_RESISTANCE, event.getEntity())
                .applyTo(event.getAmount());

        if (reduced <= 0f) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(reduced);
    }
}
