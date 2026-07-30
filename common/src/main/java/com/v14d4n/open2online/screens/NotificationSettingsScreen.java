package com.v14d4n.open2online.screens;

import com.v14d4n.open2online.server.ModServerOptions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/** Which chat messages the mod is allowed to send. Everything is on by default. */
@Environment(EnvType.CLIENT)
public class NotificationSettingsScreen extends OptionsSubScreen {
    public NotificationSettingsScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options,
                Component.translatable("gui.open2online.notifications"));
    }

    @Override
    protected void addOptions() {
        this.list.addSmall(
                ModServerOptions.updateNotifications(),
                ModServerOptions.licenseNotifications(),
                ModServerOptions.whitelistNotifications());
    }
}
