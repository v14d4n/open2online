package com.v14d4n.open2online;

import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.network.PublishTask;
import com.v14d4n.open2online.network.UPnPHandler;
import com.v14d4n.open2online.screens.AdvancedSettingsScreen;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import dev.architectury.platform.client.ConfigurationScreenRegistry;
import net.minecraft.client.gui.screens.PauseScreen;

/**
 * Everything the mod hangs off, and it all hangs off the client.
 *
 * <p>There is no side split here any more because both loaders are now told the same thing: Fabric by
 * {@code "environment": "client"} with only a client entrypoint, NeoForge by {@code dist} on its
 * {@code @Mod} annotation. Neither will call this on a dedicated server, so the {@code EnvExecutor}
 * that used to fence off the client half was guarding against something that can no longer happen.
 *
 * <p>Two of the listeners below come from Architectury's common package, which is worth reading
 * correctly: common means the event exists on both sides, not that it needs a dedicated server.
 * {@code SERVER_STOPPING} fires for any {@code MinecraftServer}, and every singleplayer world runs
 * one. It is the only thing that says "the server behind this world is going away" — including when
 * the game is closed from inside the world, which is exactly where the client-side quit event says
 * nothing.
 */
public final class OpenToOnline {
    public static final String MOD_ID = "open2online";

    private OpenToOnline() {
    }

    public static void init() {
        OpenToOnlineConfig.load();

        ConfigurationScreenRegistry.register(Platform.getMod(MOD_ID), AdvancedSettingsScreen::new);

        ClientLifecycleEvent.CLIENT_STARTED.register(client -> UpdateChecker.check());

        // A world going away calls off a publish that was still on its way to it, and closing the game
        // is one of the ways a world goes away. Registered before the close below so that a worker
        // still inside a discovery has the whole of Minecraft's three second shutdown grace to notice
        // — and it looks at this the moment the discovery ends, before anything reaches the router.
        //
        // Without it, quitting mid-publish is not a cancel at all: nothing tells the worker, so it
        // finishes, maps the port, finds the world gone and unmaps it — and if the process dies in
        // between those last two steps, the mapping stays on the router with nobody left to remove it.
        LifecycleEvent.SERVER_STOPPING.register(server -> PublishTask.cancel());

        // The mapping lives exactly as long as the server does. No player listener is needed at all:
        // logins are answered by MixinPlayerList at vanilla's own gate, and logouts say nothing the
        // server stopping does not say better.
        LifecycleEvent.SERVER_STOPPING.register(UPnPHandler::onServerStopping);

        // Registration order decides which line lands in chat first: listeners run in the order they
        // were added, and both hand their message to the same queue. The update notice goes first so
        // the auto start countdown stays the last thing said.
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> UpdateChecker.announceIfPending());
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> AutoStart.onPlayerJoin());
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> AutoStart.onPlayerQuit());
        ClientTickEvent.CLIENT_POST.register(AutoStart::onClientTick);

        // One gesture for both of the mod's waits: opening the pause menu calls off a pending auto
        // start and a publish in flight alike. The pause menu specifically — the inventory or a chest
        // must not count. Each of the two speaks only if it had something to call off.
        ClientGuiEvent.INIT_POST.register((screen, access) -> {
            if (screen instanceof PauseScreen) {
                AutoStart.cancel();
                PublishTask.cancel();
            }
        });
    }
}
