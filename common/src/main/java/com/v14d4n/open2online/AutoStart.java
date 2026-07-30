package com.v14d4n.open2online;

import com.v14d4n.open2online.commands.OpenToOnlineCommand;
import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.network.ServerHandler;
import com.v14d4n.open2online.network.chat.ModChat;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.GameType;

/**
 * Publishes the world on its own shortly after the player enters it (issue #7).
 *
 * <p>The publish is not fired straight from the join event: the client has not necessarily been told
 * its permission level yet, and the integrated server is still settling. The wait also gives the
 * player a chance to call the whole thing off.
 */
@Environment(EnvType.CLIENT)
public final class AutoStart {
    private static final int TICKS_PER_SECOND = 20;
    private static final int IDLE = -1;

    private static int countdown = IDLE;

    private AutoStart() {
    }

    public static void onPlayerJoin(LocalPlayer player) {
        countdown = IDLE;

        // Only our own world: joining someone else's server must not arm anything, nor announce it.
        if (!OpenToOnlineConfig.autoStart.get() || !Minecraft.getInstance().hasSingleplayerServer()) {
            return;
        }

        int seconds = OpenToOnlineConfig.autoStartDelay.get();
        countdown = seconds * TICKS_PER_SECOND;

        ModChat.send(ModChatTranslatableComponent.of("chat.open2online.autoStartScheduled",
                ModChatTranslatableComponent.MessageTypes.OK, seconds));
    }

    /** Leaving the world drops a pending start; there is nothing left to publish. */
    public static void onPlayerQuit(LocalPlayer player) {
        countdown = IDLE;
    }

    /**
     * Opening the pause menu calls off a pending start. The pause menu specifically, not any screen
     * — the inventory or a chest should not count.
     *
     * <p>Only the countdown is cancellable. Once the worker is off doing the port mapping there is
     * no safe way to stop it mid-flight, so that stays out of scope.
     */
    public static void onScreenOpened(Screen screen) {
        if (countdown == IDLE || !(screen instanceof PauseScreen)) {
            return;
        }

        countdown = IDLE;
        ModChat.send(ModChatTranslatableComponent.of("chat.open2online.autoStartCancelled",
                ModChatTranslatableComponent.MessageTypes.WARN));
    }

    public static void onClientTick(Minecraft minecraft) {
        if (countdown == IDLE) {
            return;
        }

        countdown--;
        if (countdown > 0) {
            return;
        }
        countdown = IDLE;

        publish(minecraft);
    }

    private static void publish(Minecraft minecraft) {
        // Someone else's server, or ours and already open — nothing to do either way.
        if (!minecraft.hasSingleplayerServer() || ServerHandler.isServerPublished()) {
            return;
        }

        // Read on the client thread and handed over as values: the worker must not poke at the
        // player or the game mode while the client keeps ticking.
        int port = OpenToOnlineConfig.port.get();
        int maxPlayers = OpenToOnlineConfig.maxPlayers.get();
        boolean online = OpenToOnlineConfig.autoStartOnline.get();
        GameType gameMode = minecraft.gameMode != null ? minecraft.gameMode.getPlayerMode() : GameType.SURVIVAL;
        boolean allowCommands = ServerHandler.canLocalPlayerUseCheats();

        Thread worker = new Thread(
                () -> OpenToOnlineCommand.open(port, maxPlayers, gameMode, allowCommands, online),
                "Open2Online auto start");
        worker.setDaemon(true);
        worker.start();
    }
}
