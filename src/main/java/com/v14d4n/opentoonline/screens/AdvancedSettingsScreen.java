package com.v14d4n.opentoonline.screens;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.v14d4n.opentoonline.server.ModServerOptions;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.TooltipAccessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Option;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Optional;

public class AdvancedSettingsScreen extends Screen {

    private static final Option[] OPTIONS = new Option[]{ ModServerOptions.ALLOW_PVP, ModServerOptions.WHITELIST_MODE };
    private OptionsList optionsList;
    private final Screen lastScreen;

    public AdvancedSettingsScreen(Screen pLastScreen) {
        super(new TranslatableComponent("gui.opentoonline.advancedServerSettings"));
        this.lastScreen = pLastScreen;
    }

    @Override
    protected void init() {
        this.optionsList = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
        this.optionsList.addBig(ModServerOptions.LIBRARY);
        this.optionsList.addSmall(OPTIONS);
        this.addWidget(this.optionsList);
        this.addRenderableWidget(new Button(this.width / 2 - 100, this.height - 27, 200, 20, CommonComponents.GUI_DONE, (p_96827_) -> {
            this.minecraft.setScreen(this.lastScreen);
            ModServerOptions.save();
        }));
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
        List<FormattedCharSequence> list = tooltipAt(this.optionsList, pMouseX, pMouseY);
        if (list != null) {
            this.renderTooltip(pPoseStack, list, pMouseX, pMouseY);
        }
    }

    private static List<FormattedCharSequence> tooltipAt(OptionsList p_96288_, int pMouseX, int pMouseY) {
        Optional<AbstractWidget> optional = p_96288_.getMouseOver(pMouseX, pMouseY);
        return optional.isPresent() && optional.get() instanceof TooltipAccessor ? ((TooltipAccessor)optional.get()).getTooltip() : ImmutableList.of();
    }
}
