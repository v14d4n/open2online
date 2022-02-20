package com.v14d4n.opentoonline.network.chat;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public class ModChatTranslatableComponent extends TextComponent {

    public ModChatTranslatableComponent(String pKey) {
        this(pKey, MessageTypes.OK);
    }

    public ModChatTranslatableComponent(String pKey, MessageTypes type) {
        super(getTranslatableComponentWithPrefix(pKey, type));
    }

    private static String getTranslatableComponentWithPrefix(String pKey, MessageTypes type) {
        String prefixColor;

        switch (type) {
            case WARN ->  prefixColor = "\u00A76";
            case ERROR ->  prefixColor = "\u00A7c";
            default -> prefixColor = "\u00A7a";
        }

        MutableComponent prefix = new TextComponent(prefixColor + "[Open2Online]\u00A7r ");
        MutableComponent text = new TranslatableComponent(pKey);
        MutableComponent chatMessage = prefix.append(text);

        return chatMessage.getString();
    }

    public enum MessageTypes {
        OK,
        WARN,
        ERROR
    }
}
