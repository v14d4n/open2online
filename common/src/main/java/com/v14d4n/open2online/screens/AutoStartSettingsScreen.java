package com.v14d4n.open2online.screens;

import com.v14d4n.open2online.server.ModServerOptions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/** Publishing the world automatically after entering it, and where to publish it. */
@Environment(EnvType.CLIENT)
public class AutoStartSettingsScreen extends OptionsSubScreen {
    public AutoStartSettingsScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options,
                Component.translatable("gui.open2online.autoStart"));
    }

    @Override
    protected void addOptions() {
        this.list.addSmall(
                ModServerOptions.autoStart(),
                ModServerOptions.autoStartMode());
        this.list.addBig(ModServerOptions.autoStartDelay());
    }
}
