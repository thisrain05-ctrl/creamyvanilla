package com.celestial878.creamyvanilla.sack;

import com.celestial878.creamyvanilla.CreamyVanilla;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Level 1 only stops sprinting (see LocalPlayerMixin). Level 2 also lowers jump height (see SacksEvents).
 * Level 3 and above also slow walking speed, which grows with the level.
 */
public class OverencumberedEffect extends MobEffect {

    public OverencumberedEffect() {
        super(MobEffectCategory.HARMFUL, 0x6C451F);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(CreamyVanilla.MODID, "overencumbered_speed_debuff"),
                -0.15F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    // The speed debuff only kicks in from the third level.
    @Override
    public void addAttributeModifiers(AttributeMap attributeMap, int amplifier) {
        if (amplifier > 1) {
            super.addAttributeModifiers(attributeMap, amplifier - 2);
        }
    }
}
