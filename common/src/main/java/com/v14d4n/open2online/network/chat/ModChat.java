package com.v14d4n.open2online.network.chat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class ModChat {
    private ModChat() {
    }

    /**
     * Safe to call from any thread.
     *
     * <p>Port mapping runs on a worker thread, and adding a chat line makes the font build glyphs
     * and upload them to a texture — that has to happen on the render thread, so the call is handed
     * to the client's task queue. The queue is FIFO, so message order is preserved.
     *
     * <p>{@code Minecraft.getInstance()} is looked up per call rather than cached in a static field
     * the way 1.16.5 did it, since that resolves at class-load time and can hand back a half-built
     * client.
     */
    public static void send(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        minecraft.execute(() -> {
            if (minecraft.gui != null) {
                minecraft.gui.getChat().addMessage(message);
            }
        });
    }
}
