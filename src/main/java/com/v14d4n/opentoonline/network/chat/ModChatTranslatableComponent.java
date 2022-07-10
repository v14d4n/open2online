package com.v14d4n.opentoonline.network.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ModChatTranslatableComponent {

    public static MutableComponent getTranslatableComponentWithPrefix(String pKey) {
        MutableComponent prefix = Component.literal("\u00A7a" + "[Open2Online]\u00A7r ");
        MutableComponent text = Component.translatable(pKey);
        MutableComponent chatMessage = prefix.append(text);

        return chatMessage;
    }

    public static MutableComponent getTranslatableComponentWithPrefix(String pKey, MessageTypes type) {
        String prefixColor;

        switch (type) {
            case WARN ->  prefixColor = "\u00A76";
            case ERROR ->  prefixColor = "\u00A7c";
            default -> prefixColor = "\u00A7a";
        }

        MutableComponent prefix = Component.literal(prefixColor + "[Open2Online]\u00A7r ");
        MutableComponent text = Component.translatable(pKey);
        MutableComponent chatMessage = prefix.append(text);

        return chatMessage;
    }

    public enum MessageTypes {
        OK,
        WARN,
        ERROR
    }
}
