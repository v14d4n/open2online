package com.v14d4n.opentoonline.server;

import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.screens.EditWhitelistScreen;
import net.minecraft.client.Option;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.function.Supplier;

import static com.v14d4n.opentoonline.OpenToOnline.minecraft;

public class WhitelistServerOptions {

    public static Option createAddFriendButton(Supplier<String> supplier) {
        return new Option("+") {
            @Override
            public AbstractWidget createButton(Options pOptions, int pX, int pY, int pWidth) {

                return new Button(pX, pY, pWidth, 20, new TranslatableComponent("+"),
                        (onClick) -> {
                            OpenToOnlineConfig.friends.get().add(supplier.get());
                            EditWhitelistScreen.update();
                        });
            }
        };
    }

    public static Option createNicknameBox(String name, boolean accessible) {
        return new Option("") {
            @Override
            public AbstractWidget createButton(Options pOptions, int pX, int pY, int pWidth) {
                EditBox editBox = new EditBox(minecraft.font, pX, pY, pWidth, 20, new TranslatableComponent(""));
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

    public static Option createRemoveFriendButton(int friendIndex) {
        return new Option("-") {
            @Override
            public AbstractWidget createButton(Options pOptions, int pX, int pY, int pWidth) {

                return new Button(pX, pY, pWidth, 20, new TranslatableComponent("-"),
                        (onClick) -> {
                            OpenToOnlineConfig.friends.get().remove(friendIndex);
                            EditWhitelistScreen.update();
                        });
            }
        };
    }
}
