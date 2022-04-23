package com.v14d4n.opentoonline.network.chat;

import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class ModChatTranslatableComponent extends StringTextComponent {

    public ModChatTranslatableComponent(String pKey) {
        this(pKey, MessageTypes.OK);
    }

    public ModChatTranslatableComponent(String pKey, MessageTypes type) {
        super(getTranslatableComponentWithPrefix(pKey, type));
    }

    private static String getTranslatableComponentWithPrefix(String pKey, MessageTypes type) {
        String prefixColor;

        switch (type) {
            case     WARN:  prefixColor = "\u00A76"; break;
            case     ERROR: prefixColor = "\u00A7c"; break;
            default:        prefixColor = "\u00A7a"; break;
        }

        IFormattableTextComponent prefix = new StringTextComponent(prefixColor + "[Open2Online]\u00A7r ");
        IFormattableTextComponent text = new TranslationTextComponent(pKey);
        IFormattableTextComponent chatMessage = prefix.append(text);

        return chatMessage.getString();
    }

    public enum MessageTypes {
        OK,
        WARN,
        ERROR
    }
}
