package com.celestial878.creamyvanilla.sack;

import com.celestial878.creamyvanilla.CreamyVanilla;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = CreamyVanilla.MODID)
public class SacksEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SackItem.applyOverencumbered(player);
        }
    }

    // The second overencumbered level and up also lowers the jump height.
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effect = entity.getEffect(SackRegistry.OVERENCUMBERED);
        if (effect != null && effect.getAmplifier() > 0) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, Math.max(0, motion.y - 0.1D), motion.z);
        }
    }
}
