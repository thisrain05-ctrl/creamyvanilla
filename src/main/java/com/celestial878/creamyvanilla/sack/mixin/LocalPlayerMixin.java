package com.celestial878.creamyvanilla.sack.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.celestial878.creamyvanilla.sack.SackRegistry;

import net.minecraft.client.player.LocalPlayer;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    // Overencumbered players can't start sprinting.
    @Inject(method = "hasEnoughImpulseToStartSprinting", at = @At("RETURN"), cancellable = true)
    private void creamyvanilla$preventSprintingWhenOverencumbered(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && ((LocalPlayer) (Object) this).hasEffect(SackRegistry.OVERENCUMBERED)) {
            cir.setReturnValue(false);
        }
    }
}
