package com.v14d4n.open2online.network;

import com.v14d4n.open2online.network.chat.ModChat;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent.MessageTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * One run of the publish, carrying the two things every step of it needs: whether the player has
 * called it off, and somewhere to speak that falls silent when they have.
 *
 * <p>Steps take this rather than a bare flag so that a step added later gets both without having to
 * be told about either. It is also what keeps the promise that a cancelled publish says nothing
 * more: {@link #say} is how the cancellable part of the path talks, and it stops answering the
 * moment the attempt is called off, instead of every message carrying a guard somebody has to
 * remember to write.
 */
@Environment(EnvType.CLIENT)
public final class PublishAttempt {
    private enum Stage {
        /** Still callable off. */
        RUNNING,
        /** Called off before the world went online. */
        CANCELLED,
        /** Handed to {@code publishServer}; there is no way back from here. */
        COMMITTED
    }

    private final AtomicReference<Stage> stage = new AtomicReference<>(Stage.RUNNING);

    /** True for whoever gets there first, and only while there is still something to call off. */
    boolean cancel() {
        return stage.compareAndSet(Stage.RUNNING, Stage.CANCELLED);
    }

    /**
     * Passes the point of no return, answering whether it was still there to pass.
     *
     * <p>Deciding and moving are one step on purpose. Asking "was this cancelled?" and then
     * publishing leaves a gap for a cancel to land in, which would put a world online that the
     * player had just called off — and then tell them it had been cancelled.
     */
    boolean commit() {
        return stage.compareAndSet(Stage.RUNNING, Stage.COMMITTED);
    }

    boolean isCancelled() {
        return stage.get() == Stage.CANCELLED;
    }

    void say(String key) {
        say(key, MessageTypes.OK);
    }

    void say(String key, MessageTypes type, Object... args) {
        say(ModChatTranslatableComponent.of(key, type, args));
    }

    /** For the lines that have something appended to them. */
    void say(Component message) {
        if (isCancelled()) {
            return;
        }
        ModChat.send(message);
    }
}
