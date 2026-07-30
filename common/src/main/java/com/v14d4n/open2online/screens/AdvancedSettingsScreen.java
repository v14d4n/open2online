package com.v14d4n.open2online.screens;

import com.v14d4n.open2online.server.ModServerOptions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/**
 * Extends {@link OptionsSubScreen} rather than {@code Screen}: since 1.21 {@code OptionsList} only
 * accepts an {@code OptionsSubScreen}, and the base class already supplies the title, the scrolling
 * list, the footer with Done, and the way back to the previous screen.
 */
@Environment(EnvType.CLIENT)
public class AdvancedSettingsScreen extends OptionsSubScreen {
    public AdvancedSettingsScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options,
                Component.translatable("gui.open2online.advancedServerSettings"));
    }

    /**
     * Layout mirrors the 1.16.5 screen: the library picker spans the full width, then the four
     * remaining entries fill two rows of two columns in this exact order.
     */
    @Override
    protected void addOptions() {
        this.list.addBig(ModServerOptions.library());
        this.list.addSmall(
                createEditWhitelistButton(),
                ModServerOptions.allowPvp().createButton(this.options));
        this.list.addSmall(
                ModServerOptions.updateNotifications().createButton(this.options),
                ModServerOptions.hideIP().createButton(this.options));

        // Full width rather than squeezed into a half row: it changes who can get onto the server,
        // and an odd count would leave it dangling in the left column anyway.
        this.list.addBig(ModServerOptions.requireLicense());
    }

    private Button createEditWhitelistButton() {
        return Button.builder(Component.translatable("gui.open2online.editWhitelist"),
                        press -> this.minecraft.setScreen(new EditWhitelistScreen(this)))
                .build();
    }
}
