package com.v14d4n.opentoonline.screens;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.server.ModServerOptions;
import com.v14d4n.opentoonline.server.WhitelistServerOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.IBidiTooltip;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.OptionsRowList;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class EditWhitelistScreen extends Screen {

    private OptionsRowList optionsList;
    private static Screen lastScreen = null;
    private static TextFieldWidget editBox = null;

    public EditWhitelistScreen(Screen pLastScreen) {
        super(new TranslationTextComponent("gui.opentoonline.editWhitelist"));
        lastScreen = pLastScreen;
    }

    @Override
    protected void init() {
        this.optionsList = new OptionsRowList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
        this.optionsList.addBig(ModServerOptions.WHITELIST_MODE);

        // creates top widgets
        this.optionsList.addSmall(
                WhitelistServerOptions.createNicknameBox("", true),
                WhitelistServerOptions.createAddFriendButton(() -> editBox.getValue())
        );

        // creates list of whitelisted players
        for (int i = 0; i < OpenToOnlineConfig.friends.get().size(); i++) {
            this.optionsList.addSmall(
                    WhitelistServerOptions.createNicknameBox(OpenToOnlineConfig.friends.get().get(i), false),
                    WhitelistServerOptions.createRemoveFriendButton(i)
            );
        }

        this.addWidget(this.optionsList);
        this.addButton(new Button(this.width / 2 - 100, this.height - 27, 200, 20, DialogTexts.GUI_DONE, (p_96827_) -> {
            this.minecraft.setScreen(lastScreen);
        }));

        this.setInitialFocus(editBox);
    }

    @Override
    public void tick() {
        // update editBox
        editBox.tick();
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

    public static void update() {
        Minecraft.getInstance().setScreen(new EditWhitelistScreen(EditWhitelistScreen.lastScreen));
    }

    public static void setActiveEditBox(TextFieldWidget editBox) {
        EditWhitelistScreen.editBox = editBox;
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        return editBox.charTyped(pCodePoint, pModifiers);
    }
}
