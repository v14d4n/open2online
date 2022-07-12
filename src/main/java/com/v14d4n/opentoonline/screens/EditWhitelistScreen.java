package com.v14d4n.opentoonline.screens;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Optional;

public class EditWhitelistScreen extends Screen {

    private OptionsList optionsList;
    private static Screen lastScreen = null;
    private static EditBox editBox = null;

    public EditWhitelistScreen(Screen pLastScreen) {
        super(Component.translatable("gui.opentoonline.editWhitelist"));
        lastScreen = pLastScreen;
    }

    @Override
    protected void init() {
        this.optionsList = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);

        // creates top widgets
//        this.optionsList.addSmall(
//                WhitelistServerOptions.createNicknameBox("", true),
//                WhitelistServerOptions.createAddFriendButton(() -> editBox.getValue())
//        );

        // creates list of whitelisted players
        for (int i = 0; i < OpenToOnlineConfig.friends.get().size(); i++) {
//            this.optionsList.addSmall(
//                    WhitelistServerOptions.createNicknameBox(OpenToOnlineConfig.friends.get().get(i), false),
//                    WhitelistServerOptions.createRemoveFriendButton(i)
//            );
        }

        this.addWidget(this.optionsList);
        this.addRenderableWidget(new Button(this.width / 2 - 100, this.height - 27, 200, 20, CommonComponents.GUI_DONE, (p_96827_) -> {
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
        return optional.isPresent() && optional.get() instanceof TooltipAccessor ? ((TooltipAccessor) optional.get()).getTooltip() : ImmutableList.of();
    }

    public static void update() {
        Minecraft.getInstance().setScreen(new EditWhitelistScreen(EditWhitelistScreen.lastScreen));
    }

    public static void setActiveEditBox(EditBox editBox) {
        EditWhitelistScreen.editBox = editBox;
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        return editBox.charTyped(pCodePoint, pModifiers);
    }
}
