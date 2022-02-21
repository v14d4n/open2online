package com.v14d4n.opentoonline.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Option;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;

public class AdvancedSettingsScreen extends Screen {

    private final Screen lastScreen;
    private static final Option[] OPTIONS = new Option[]{Option.GRAPHICS, Option.RENDER_DISTANCE};
    private OptionsList optionsList;

    private static boolean allowPvp = false;

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
