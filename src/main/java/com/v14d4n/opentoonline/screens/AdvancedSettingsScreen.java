package com.v14d4n.opentoonline.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.v14d4n.opentoonline.server.ModServerOption;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Option;
import net.minecraft.network.chat.TranslatableComponent;

// TODO: доделать
public class AdvancedSettingsScreen extends Screen {

    private final Screen lastScreen;
    private static final Option[] OPTIONS = new Option[]{ ModServerOption.ALLOW_PVP, ModServerOption.LICENSE_REQUIRED };
    private OptionsList optionsList;

    public AdvancedSettingsScreen(Screen pLastScreen) {
        super(new TranslatableComponent("gui.opentoonline.advancedServerSettings"));
        this.lastScreen = pLastScreen;
    }

    @Override
    protected void init() {
        this.optionsList = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
        this.optionsList.addSmall(OPTIONS);
        this.addWidget(this.optionsList);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(lastScreen);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);
        this.optionsList.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 13, 16777215);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }
}
