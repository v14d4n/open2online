package com.v14d4n.open2online.screens;

import com.v14d4n.open2online.server.ModServerOptions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Publishing the world automatically after entering it, and where to publish it. */
@Environment(EnvType.CLIENT)
public class AutoStartSettingsScreen extends ModOptionsScreen {
    public AutoStartSettingsScreen(Screen lastScreen) {
        super(lastScreen, Component.translatable("gui.open2online.autoStart"));
    }

    @Override
    protected void addOptions() {
        OptionsList optionsList = optionsList();

        optionsList.addSmall(
                ModServerOptions.autoStart(),
                ModServerOptions.autoStartMode());
        optionsList.addBig(ModServerOptions.autoStartDelay());
    }
}
