package com.v14d4n.open2online.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Built on vanilla's {@link PopupScreen}: a framed dialog drawn over the screen behind it, with a
 * title, a wrapped message and a row of buttons.
 *
 * <p>This is the shape mods actually use for this kind of notice — Sodium, for instance, hand-rolls
 * the same thing as a fixed-size prompt with one action button rather than a full page. A whole
 * screen of centred text with big stacked buttons reads as far heavier than the message deserves.
 */
@Environment(EnvType.CLIENT)
public final class SupportDeveloperPopup {
    /** The entry point is offered to Russian players only, so the popup is gated on the same code. */
    private static final String LANGUAGE_CODE = "ru_ru";

    private static final String BOOSTY_URL = "https://boosty.to/v14d4n/donate";
    private static final int WIDTH = 300;

    private SupportDeveloperPopup() {
    }

    public static boolean isAvailable() {
        return LANGUAGE_CODE.equals(Minecraft.getInstance().getLanguageManager().getSelected());
    }

    public static Screen create(Screen background) {
        return new PopupScreen.Builder(background, Component.translatable("gui.open2online.supportDeveloper"))
                .setMessage(Component.translatable("gui.open2online.support.message"))
                .setWidth(WIDTH)
                // confirmLinkNow shows vanilla's "open this website?" prompt, hands the address to the
                // browser and comes back here — no need to wire the callback by hand.
                .addButton(Component.translatable("gui.open2online.support.boosty"),
                        popup -> ConfirmLinkScreen.confirmLinkNow(popup, BOOSTY_URL, false))
                .addButton(CommonComponents.GUI_DONE, popup -> close(background))
                .onClose(() -> close(background))
                .build();
    }

    private static void close(Screen background) {
        Minecraft.getInstance().setScreen(background);
    }
}
