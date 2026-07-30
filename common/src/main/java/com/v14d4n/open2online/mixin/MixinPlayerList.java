package com.v14d4n.open2online.mixin;

import com.v14d4n.open2online.network.ServerHandler;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hands the whitelist decision to vanilla's login gate.
 *
 * <p>1.16.5 let the player finish joining and then disconnected them from a player-join event, so
 * they briefly entered the world and the server spawned them for nothing. {@code canPlayerLogin}
 * asks {@code isWhiteListed} before any of that, and produces a translated rejection message, so the
 * mod only has to answer the membership question.
 *
 * <p>The native {@code whitelist.json} is deliberately left untouched — it keys entries by UUID and
 * its path is relative to the game directory, which would make it shared across every world.
 */
@Mixin(PlayerList.class)
public abstract class MixinPlayerList {
    /**
     * Only reports "in use" while this client is hosting an online server with the option on, so
     * ordinary singleplayer and LAN-only sessions keep vanilla behaviour. Read per call rather than
     * latched at publish time, so toggling the option applies to the next player who connects.
     */
    @Inject(method = "isUsingWhitelist", at = @At("HEAD"), cancellable = true)
    private void open2online$whitelistInUse(CallbackInfoReturnable<Boolean> cir) {
        if (ServerHandler.isClientRunningOnlineServer()) {
            cir.setReturnValue(ServerHandler.isWhitelistActive());
        }
    }

    @Inject(method = "isWhiteListed", at = @At("HEAD"), cancellable = true)
    private void open2online$whitelisted(NameAndId nameAndId, CallbackInfoReturnable<Boolean> cir) {
        if (ServerHandler.isWhitelistActive()) {
            cir.setReturnValue(ServerHandler.isWhitelisted(nameAndId));
        }
    }
}
