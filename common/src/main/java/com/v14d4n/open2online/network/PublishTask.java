package com.v14d4n.open2online.network;

import com.v14d4n.open2online.network.chat.ModChat;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent.MessageTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.GameType;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Publishes the world, and keeps a second attempt from starting while one is under way.
 *
 * <p>Publishing takes seconds — backend discovery, the port mapping, then the address lookup — and
 * runs on a worker while the player keeps playing, so it can be asked for again in the meantime:
 * the screen is one pause menu away from being reopened.
 *
 * <p>Everything arrives as values. Callers read the player and the game mode on the client thread
 * and hand the results over, because by the time this runs the client is ticking again.
 */
@Environment(EnvType.CLIENT)
public final class PublishTask {
    private static final AtomicBoolean running = new AtomicBoolean();

    private PublishTask() {
    }

    public static void start(int port, int maxPlayers, GameType gameMode, boolean allowCommands, boolean online) {
        if (!running.compareAndSet(false, true)) {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.error.alreadyStarting",
                    MessageTypes.ERROR));
            return;
        }

        try {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.startingServer"));

            if (ServerHandler.isServerPublished()) {
                ModChat.send(ModChatTranslatableComponent.of("chat.open2online.error.serverIsAlreadyPublished",
                        MessageTypes.ERROR));
                return;
            }

            if (online && !UPnPHandler.isPortAvailable(port)) {
                ModChat.send(ModChatTranslatableComponent.of("chat.open2online.error.publishFailed",
                        MessageTypes.ERROR));
                return;
            }

            ServerHandler.startServer(port, maxPlayers, gameMode, allowCommands, online);
        } finally {
            running.set(false);
        }
    }
}
