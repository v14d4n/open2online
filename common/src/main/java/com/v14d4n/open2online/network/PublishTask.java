package com.v14d4n.open2online.network;

import com.v14d4n.open2online.network.chat.ModChat;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent.MessageTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;

import java.util.concurrent.atomic.AtomicReference;

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
    /**
     * The attempt a new start would collide with, or null when there is none to collide with.
     *
     * <p>Note what this is not: it is not "a worker is running". A cancelled attempt gives this up
     * at once, while its worker may still be several seconds from noticing — sitting inside a
     * discovery that cannot be interrupted. That is deliberate. Nothing has been done to the router
     * yet at that point, so there is nothing for the next publish to wait for, and refusing it
     * because a doomed attempt has not finished dying would be a lie about the state of things.
     */
    private static final AtomicReference<PublishAttempt> current = new AtomicReference<>();

    private PublishTask() {
    }

    public static void start(int port, int maxPlayers, GameType gameMode, boolean allowCommands, boolean online) {
        PublishAttempt attempt = new PublishAttempt();
        if (!current.compareAndSet(null, attempt)) {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.error.alreadyStarting",
                    MessageTypes.ERROR));
            return;
        }

        try {
            attempt.say("chat.open2online.startingServer");

            if (ServerHandler.isServerPublished()) {
                attempt.say("chat.open2online.error.serverIsAlreadyPublished", MessageTypes.ERROR);
                return;
            }

            // Asked for both destinations: publishing binds the port either way, so a busy one
            // stops a LAN game exactly as it stops an online one. Vanilla's own check, the one
            // ShareToLanScreen validates its port with — it binds a ServerSocket, which is the real
            // question, rather than probing for something listening.
            if (!HttpUtil.isPortAvailable(port)) {
                attempt.say("chat.open2online.error.portInUse", MessageTypes.ERROR, port);
                return;
            }

            ServerHandler.startServer(port, maxPlayers, gameMode, allowCommands, online, attempt);
        } finally {
            // Only if it is still ours. A cancel hands the slot back immediately, so by the time a
            // called-off worker gets here the player may have started another publish — clearing
            // that one's claim would let a third start walk straight past the guard above.
            current.compareAndSet(attempt, null);
        }
    }

    /**
     * Calls off a publish in flight, if there is one that can still be called off.
     *
     * <p>Says so straight away rather than when the work actually stops, because the two are not the
     * same moment: none of the port mapping libraries can be interrupted, so a worker may take
     * several more seconds to reach a point where it can look at this. From the player's side that
     * gap does not exist — the publish is over, and starting another one is allowed at once.
     *
     * <p>Silent when there is nothing to call off, including when the world has already gone online:
     * past that point there is no undoing it, and saying otherwise would be false.
     */
    public static void cancel() {
        PublishAttempt attempt = current.get();
        if (attempt == null || !attempt.cancel()) {
            return;
        }

        current.compareAndSet(attempt, null);
        ModChat.send(ModChatTranslatableComponent.of("chat.open2online.publishCancelled",
                MessageTypes.WARN));
    }
}
