package com.v14d4n.open2online;

import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.network.PublishTask;
import com.v14d4n.open2online.network.ServerHandler;
import com.v14d4n.open2online.network.chat.ModChat;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Util;
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
    private static final long MILLIS_PER_SECOND = 1000L;
    /** Out of reach of any real timestamp, so an armed deadline can never be mistaken for this. */
    private static final long IDLE = Long.MIN_VALUE;

    /**
     * When the publish is due, as a {@link Util#getMillis()} reading, or {@link #IDLE}.
     *
     * <p>A deadline rather than a countdown of ticks: the client tick rate is a target, not a
     * promise, and under load ticks arrive late, which stretches the configured seconds into
     * something longer. {@code getMillis()} reads a nano time source, so the deadline survives both
     * a stuttering game and the wall clock being moved.
     *
     * <p>Every event that touches this runs on the client thread, so plain access is enough.
     */
    private static long publishAt = IDLE;

    private AutoStart() {
    }

    public static void onPlayerJoin() {
        publishAt = IDLE;

        // Only our own world: joining someone else's server must not arm anything, nor announce it.
        if (!OpenToOnlineConfig.autoStart.get() || !Minecraft.getInstance().hasSingleplayerServer()) {
            return;
        }

        int seconds = OpenToOnlineConfig.autoStartDelay.get();
        publishAt = Util.getMillis() + seconds * MILLIS_PER_SECOND;

        ModChat.send(ModChatTranslatableComponent.of("chat.open2online.autoStartScheduled",
                ModChatTranslatableComponent.MessageTypes.OK, seconds));
    }

    /** Leaving the world drops a pending start; there is nothing left to publish. */
    public static void onPlayerQuit() {
        publishAt = IDLE;
    }

    /**
     * Opening the pause menu calls off a pending start. The pause menu specifically, not any screen
     * — the inventory or a chest should not count.
     *
     * <p>Only the countdown is cancellable. Once the worker is off doing the port mapping there is
     * no safe way to stop it mid-flight, so that stays out of scope.
     */
    public static void onScreenOpened(Screen screen) {
        if (publishAt == IDLE || !(screen instanceof PauseScreen)) {
            return;
        }

        publishAt = IDLE;
        ModChat.send(ModChatTranslatableComponent.of("chat.open2online.autoStartCancelled",
                ModChatTranslatableComponent.MessageTypes.WARN));
    }

    /**
     * The tick only asks whether the deadline has passed; it is the client thread's turn to speak
     * that this borrows, not a clock of its own.
     */
    public static void onClientTick(Minecraft minecraft) {
        if (publishAt == IDLE || Util.getMillis() < publishAt) {
            return;
        }

        publishAt = IDLE;
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

        Util.ioPool().execute(() -> PublishTask.start(port, maxPlayers, gameMode, allowCommands, online));
    }
}
