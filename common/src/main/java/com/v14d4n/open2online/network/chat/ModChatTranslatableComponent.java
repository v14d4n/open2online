package com.v14d4n.open2online.network.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Builds the mod's prefixed chat lines.
 *
 * <p>On 1.16.5 this was a {@code StringTextComponent} subclass that resolved the translation
 * eagerly through {@code getString()}. {@code MutableComponent} is final now, and resolving at
 * construction time would pin the message to whatever language was active back then, so this is a
 * factory returning a lazily translated component instead.
 */
public final class ModChatTranslatableComponent {
    private static final String PREFIX = "[Open2Online]";

    private ModChatTranslatableComponent() {
    }

    public static MutableComponent of(String key) {
        return of(key, MessageTypes.OK);
    }

    /**
     * The root stays unstyled on purpose: children inherit the parent's style, so colouring the
     * root would tint the message and everything callers append to it. Only the prefix carries a
     * colour, which is what the old {@code §a…§r} form achieved with an explicit reset.
     */
    public static MutableComponent of(String key, MessageTypes type) {
        return Component.empty()
                .append(Component.literal(PREFIX).withStyle(type.color))
                .append(Component.literal(" "))
                .append(Component.translatable(key));
    }

    public enum MessageTypes {
        OK(ChatFormatting.GREEN),
        WARN(ChatFormatting.GOLD),
        ERROR(ChatFormatting.RED);

        private final ChatFormatting color;

        MessageTypes(ChatFormatting color) {
            this.color = color;
        }
    }
}
