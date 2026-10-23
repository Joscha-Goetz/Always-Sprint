package de.mirador.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.KeyMapping;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayermixin {
    //? if >=1.19.4 {
    @WrapOperation(
        method = "aiStep",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z")
    )
    private boolean alwayssprint$replaceSprintBlock(LocalPlayer player, Operation<Boolean> original) {
        if (original.call(player)) {
            player.setSprinting(true);
        }
        return false;
    }
    //?} else {
    /*@WrapOperation(
        method = "aiStep",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z", ordinal = 1)
    )
    private boolean alwayssprint$forceSprintKey(KeyMapping key, Operation<Boolean> original) {
        return true;
    }
    *///?}
}
