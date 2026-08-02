package com.v14d4n.open2online;

import com.v14d4n.open2online.commands.OpenToOnlineCommand;
import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.network.UPnPHandler;
import com.v14d4n.open2online.screens.AdvancedSettingsScreen;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import dev.architectury.platform.client.ConfigurationScreenRegistry;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;

public final class OpenToOnline {
    public static final String MOD_ID = "open2online";

    private OpenToOnline() {
    }

    public static void init() {
        OpenToOnlineConfig.load();

        // Only the host leaving matters here, to take the port mapping down with them. Joining needs
        // no server-side listener: the whitelist is answered through MixinPlayerList, at vanilla's
        // own login gate.
        PlayerEvent.PLAYER_QUIT.register(UPnPHandler::onPlayerLoggedOut);

        // Nested lambda on purpose: the outer supplier only runs on the client, so the screen class
        // is never loaded on a dedicated server.
        EnvExecutor.runInEnv(Env.CLIENT, () -> OpenToOnline::initClient);
    }

    private static void initClient() {
        ConfigurationScreenRegistry.register(Platform.getMod(MOD_ID), AdvancedSettingsScreen::new);

        // Client-side on purpose — see OpenToOnlineCommand#register.
        ClientCommandRegistrationEvent.EVENT.register(
                (dispatcher, context) -> OpenToOnlineCommand.register(dispatcher));

        ClientLifecycleEvent.CLIENT_STARTED.register(client -> UpdateChecker.check());

        // Registration order decides which line lands in chat first: listeners run in the order they
        // were added, and both hand their message to the same queue. The update notice goes first so
        // the auto start countdown stays the last thing said.
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> UpdateChecker.announceIfPending());
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> AutoStart.onPlayerJoin());
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> AutoStart.onPlayerQuit());
        ClientTickEvent.CLIENT_POST.register(AutoStart::onClientTick);
        ClientGuiEvent.INIT_POST.register((screen, access) -> AutoStart.onScreenOpened(screen));
    }
}
