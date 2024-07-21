package com.v14d4n.opentoonline.screens;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.v14d4n.opentoonline.OpenToOnline;
import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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
        //        for (int i = 0; i < OpenToOnlineConfig.friends.get().size(); i++) {
        //            this.optionsList.addSmall(
        //                    WhitelistServerOptions.createNicknameBox(OpenToOnlineConfig.friends.get().get(i), false),
        //                    WhitelistServerOptions.createRemoveFriendButton(i)
        //            );
        //        }

        Button.Builder backButtonBuilder = new Button.Builder(CommonComponents.GUI_DONE, (p_96827_) -> {
            this.minecraft.setScreen(this.lastScreen);
        });
        backButtonBuilder.pos(this.width / 2 - 100, this.height - 27);
        backButtonBuilder.size(200, 20);

        this.addWidget(this.optionsList);
        this.addRenderableWidget(backButtonBuilder.build());

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
//        List<FormattedCharSequence> list = tooltipAt(this.optionsList, pMouseX, pMouseY);
//        if (list != null) {
//            this.renderTooltip(pPoseStack, list, pMouseX, pMouseY);
//        }
    }

//    private static List<FormattedCharSequence> tooltipAt(OptionsList p_96288_, int pMouseX, int pMouseY) {
//        Optional<AbstractWidget> optional = p_96288_.getMouseOver(pMouseX, pMouseY);
//
//        if (optional.isPresent()) {
//            AbstractWidget widget = optional.get();
//            try {
//                Field tooltipField = FieldUtils.getDeclaredField(AbstractWidget.class, "tooltip", true); // Works only if you run client from IDE
//                // Field tooltipField = FieldUtils.getDeclaredField(AbstractWidget.class, "f_256816_", true); // Works only in build version
//                Tooltip tooltip = (Tooltip) tooltipField.get(widget);
//                return tooltip != null ? tooltip.toCharSequence(Minecraft.getInstance()) : ImmutableList.of();
//            } catch (IllegalAccessException e) {
//                Logger logger = LogManager.getLogManager().getLogger(OpenToOnline.MOD_ID);
//                logger.log(Level.WARNING, e.getMessage());
//            }
//        }
//
//        return ImmutableList.of();
//    }

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
