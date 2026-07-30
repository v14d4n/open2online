package com.v14d4n.open2online.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps a second publish from starting while one is already under way.
 *
 * <p>Publishing takes seconds — backend discovery, the port mapping, then the address lookup — and
 * runs on a worker while the player keeps playing, so the command can easily be fired again in the
 * meantime.
 */
@Environment(EnvType.CLIENT)
public final class PublishTask {
    private static final AtomicBoolean running = new AtomicBoolean();

    private PublishTask() {
    }

    /** Returns false when another publish is already under way. */
    public static boolean begin() {
        return running.compareAndSet(false, true);
    }

    public static void finish() {
        running.set(false);
    }

    public static boolean isRunning() {
        return running.get();
    }
}
