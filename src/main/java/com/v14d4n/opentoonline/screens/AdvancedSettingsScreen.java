package com.v14d4n.opentoonline.screens;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.v14d4n.opentoonline.server.ModServerOptions;
import net.minecraft.client.AbstractOption;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.IBidiTooltip;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoSettingsScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.OptionsRowList;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class AdvancedSettingsScreen extends Screen {

    private static final AbstractOption[] OPTIONS = new AbstractOption[]{ ModServerOptions.EDIT_WHITELIST, ModServerOptions.ALLOW_PVP };
    private OptionsRowList optionsList;
    private final Screen lastScreen;

    public AdvancedSettingsScreen(Screen pLastScreen) {
        super(new TranslationTextComponent("gui.opentoonline.advancedServerSettings"));
        this.lastScreen = pLastScreen;
    }

    @Override
    protected void init() {
        ModServerOptions.update();
        this.optionsList = new OptionsRowList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
        this.optionsList.addBig(ModServerOptions.LIBRARY);
        this.optionsList.addSmall(OPTIONS);
        this.addWidget(this.optionsList);
        this.addButton(new Button(this.width / 2 - 100, this.height - 27, 200, 20, DialogTexts.GUI_DONE, (p_96827_) -> {
            ModServerOptions.save();
            this.minecraft.setScreen(this.lastScreen);
        }));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(lastScreen);
    }

    @Override
    public void render(MatrixStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);
        this.optionsList.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 13, 16777215);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        List<IReorderingProcessor> list = tooltipAt(this.optionsList, pMouseX, pMouseY);
        if (list != null) {
            this.renderTooltip(pPoseStack, list, pMouseX, pMouseY);
        }
    }

    @Nullable
    public static List<IReorderingProcessor> tooltipAt(OptionsRowList pOptions, int pX, int pY) {
        Optional<Widget> optional = pOptions.getMouseOver((double)pX, (double)pY);
        if (optional.isPresent() && optional.get() instanceof IBidiTooltip) {
            Optional<List<IReorderingProcessor>> optional1 = ((IBidiTooltip)optional.get()).getTooltip();
            return optional1.orElse((List<IReorderingProcessor>)null);
        } else {
            return null;
        }
    }
}
