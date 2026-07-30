package com.v14d4n.open2online;

import com.v14d4n.open2online.commands.OpenToOnlineCommand;
import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.network.ServerHandler;
import com.v14d4n.open2online.network.UPnPHandler;
import com.v14d4n.open2online.screens.AdvancedSettingsScreen;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import dev.architectury.platform.client.ConfigurationScreenRegistry;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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

    private static void onPlayerJoin(ServerPlayer player) {
        String joinedName = player.getName().getString();
        String hostName = Minecraft.getInstance().getUser().getName();

        if (hostName.equals(joinedName)) {
            UpdateChecker.checkOnce();
            return;
        }

        // Only the host enforces the whitelist, and only while the world is actually exposed.
        if (!ServerHandler.isClientRunningOnlineServer()
                || !ServerHandler.isPlayerServerOwner(Minecraft.getInstance().getGameProfile())) {
            return;
        }

        if (OpenToOnlineConfig.whitelistMode.get() && !isWhitelisted(joinedName)) {
            player.connection.disconnect(Component.literal("Not in the whitelist"));
        }
    }

    private static boolean isWhitelisted(String playerName) {
        for (String friend : OpenToOnlineConfig.friends.get()) {
            if (friend.equals(playerName)) {
                return true;
            }
        }
        return false;
    }
}
