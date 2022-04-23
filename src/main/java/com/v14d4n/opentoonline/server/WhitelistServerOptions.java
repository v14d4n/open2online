package com.v14d4n.opentoonline.server;

import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.screens.EditWhitelistScreen;
import net.minecraft.client.AbstractOption;
import net.minecraft.client.GameSettings;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.function.Supplier;

import static com.v14d4n.opentoonline.OpenToOnline.minecraft;

public class WhitelistServerOptions {

    public static AbstractOption createAddFriendButton(Supplier<String> supplier) {
        return new AbstractOption("+") {

            @Override
            public Widget createButton(GameSettings pOptions, int pX, int pY, int pWidth) {
                return new Button(pX, pY, pWidth, 20, new TranslationTextComponent("+"),
                        (onClick) -> {
                            OpenToOnlineConfig.friends.get().add(supplier.get());
                            EditWhitelistScreen.update();
                        });
            }
        };
    }

    public static AbstractOption createNicknameBox(String name, boolean accessible) {
        return new AbstractOption("") {
            @Override
            public Widget createButton(GameSettings pOptions, int pX, int pY, int pWidth) {
                TextFieldWidget editBox = new TextFieldWidget(minecraft.font, pX, pY, pWidth, 20, new TranslationTextComponent(""));
                editBox.setCanLoseFocus(false);
                editBox.setEditable(accessible);
                editBox.setValue(name);
                if (accessible) {
                    EditWhitelistScreen.setActiveEditBox(editBox);
                }
                return editBox;
            }
        };
    }

    public static AbstractOption createRemoveFriendButton(int friendIndex) {
        return new AbstractOption("-") {
            @Override
            public Widget createButton(GameSettings pOptions, int pX, int pY, int pWidth) {

                return new Button(pX, pY, pWidth, 20, new TranslationTextComponent("-"),
                        (onClick) -> {
                            OpenToOnlineConfig.friends.get().remove(friendIndex);
                            EditWhitelistScreen.update();
                        });
            }
        };
    }
}
