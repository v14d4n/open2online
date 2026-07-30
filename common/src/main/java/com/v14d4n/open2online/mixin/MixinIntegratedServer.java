package com.v14d4n.open2online.mixin;

import com.v14d4n.open2online.config.OpenToOnlineConfig;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the configured player limit.
 *
 * <p>1.16.5 wrote into {@code PlayerList.maxPlayers} by reflection. That field is gone in 1.21.11 —
 * {@code IntegratedServer} holds a {@code MAX_PLAYERS} constant instead and nothing exposes a
 * setter, so overriding the getter is the only remaining seam. {@code PlayerList.canPlayerLogin}
 * compares the player count against this value, so the login limit and the advertised slot count
 * both follow from here.
 */
@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer {
    @Inject(method = "getMaxPlayers", at = @At("HEAD"), cancellable = true)
    private void open2online$configuredMaxPlayers(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(OpenToOnlineConfig.maxPlayers.get());
    }
}
