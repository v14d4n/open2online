package com.v14d4n.open2online;

import com.v14d4n.open2online.commands.OpenToOnlineCommand;
import com.v14d4n.open2online.network.UPnPHandler;
import com.v14d4n.open2online.screens.AdvancedSettingsScreen;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import dev.architectury.platform.client.ConfigurationScreenRegistry;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

public final class OpenToOnline {
    public static final String MOD_ID = "open2online";

    private OpenToOnline() {
    }

    public static void init() {
        CommandRegistrationEvent.EVENT.register(
                (dispatcher, registry, selection) -> OpenToOnlineCommand.register(dispatcher));

        PlayerEvent.PLAYER_JOIN.register(OpenToOnline::onPlayerJoin);
        PlayerEvent.PLAYER_QUIT.register(UPnPHandler::onPlayerLoggedOut);

        // Nested lambda on purpose: the outer supplier only runs on the client, so the screen class
        // is never loaded on a dedicated server.
        EnvExecutor.runInEnv(Env.CLIENT, () -> OpenToOnline::initClient);
    }

    private static void initClient() {
        ConfigurationScreenRegistry.register(Platform.getMod(MOD_ID), AdvancedSettingsScreen::new);
    }

    /**
     * The whitelist is no longer enforced from here — {@code MixinPlayerList} answers vanilla's login
     * gate instead, which rejects before the player is placed in the world.
     */
    private static void onPlayerJoin(ServerPlayer player) {
        String hostName = Minecraft.getInstance().getUser().getName();
        if (hostName.equals(player.getName().getString())) {
            UpdateChecker.checkOnce();
        }
    }
}
