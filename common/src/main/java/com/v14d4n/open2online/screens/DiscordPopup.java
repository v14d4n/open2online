package com.v14d4n.open2online.screens;

import com.v14d4n.open2online.UpdateChecker;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class DiscordPopup {
    private static final int WIDTH = 300;

    private DiscordPopup() {
    }

    public static boolean isAvailable() {
        return UpdateChecker.discordInvite().isPresent();
    }

    public static Screen create(Screen background) {
        // Safe to insist on: the button that leads here is inactive until the lookup has answered,
        // and the answer is only ever set, never taken away.
        String invite = UpdateChecker.discordInvite().orElseThrow();

        return new PopupScreen.Builder(background, Component.translatable("gui.open2online.discord"))
                .setMessage(Component.translatable("gui.open2online.discord.message"))
                .setWidth(WIDTH)
                .addButton(Component.translatable("gui.open2online.discord.join"),
                        popup -> ConfirmLinkScreen.confirmLinkNow(popup, invite, false))
                .addButton(CommonComponents.GUI_DONE, popup -> close(background))
                .onClose(() -> close(background))
                .build();
    }

    private static void close(Screen background) {
        Minecraft.getInstance().setScreen(background);
    }
}
