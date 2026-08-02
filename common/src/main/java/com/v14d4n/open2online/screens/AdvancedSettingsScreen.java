package com.v14d4n.open2online.screens;

import com.v14d4n.open2online.server.ModServerOptions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Extends {@code OptionsSubScreen} rather than {@code Screen}: since 1.21 {@code OptionsList} only
 * accepts an {@code OptionsSubScreen}, and the base class already supplies the title, the scrolling
 * list, the footer with Done, and the way back to the previous screen.
 */
@Environment(EnvType.CLIENT)
public class AdvancedSettingsScreen extends ModOptionsScreen {
    private static final int BUTTON_HEIGHT = 20;

    public AdvancedSettingsScreen(Screen lastScreen) {
        super(lastScreen, Component.translatable("gui.open2online.advancedServerSettings"));
    }

    /**
     * Layout mirrors the 1.16.5 screen: the library picker spans the full width, then the four
     * remaining entries fill two rows of two columns in this exact order.
     */
    @Override
    protected void addOptions() {
        OptionsList optionsList = optionsList();

        optionsList.addBig(ModServerOptions.library());
        // Buttons that open another screen first, then the plain toggles.
        optionsList.addSmall(
                createEditWhitelistButton(),
                createNotificationsButton());
        // Alone on its row: the second widget may be null, which is how vanilla lays out an odd
        // number of options, and the button is built at the full row width to fill it.
        optionsList.addSmall(createAutoStartButton(optionsList), null);
        optionsList.addSmall(
                ModServerOptions.allowPvp().createButton(this.options),
                ModServerOptions.hideIP().createButton(this.options));
        optionsList.addBig(ModServerOptions.requireLicense());
    }

    /**
     * Sized from {@code getRowWidth()} rather than a literal: {@code BIG_BUTTON_WIDTH}, which
     * {@code addBig} uses, is private.
     */
    private Button createAutoStartButton(OptionsList optionsList) {
        return Button.builder(Component.translatable("gui.open2online.autoStart"),
                        press -> this.minecraft.setScreen(new AutoStartSettingsScreen(this)))
                .size(optionsList.getRowWidth(), BUTTON_HEIGHT)
                .build();
    }

    private Button createNotificationsButton() {
        return Button.builder(Component.translatable("gui.open2online.notifications"),
                        press -> this.minecraft.setScreen(new NotificationSettingsScreen(this)))
                .build();
    }

    private Button createEditWhitelistButton() {
        return Button.builder(Component.translatable("gui.open2online.editWhitelist"),
                        press -> this.minecraft.setScreen(new EditWhitelistScreen(this)))
                .build();
    }
}
