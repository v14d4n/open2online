package com.v14d4n.open2online.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * What every options screen in the mod would otherwise repeat.
 *
 * <p>{@code OptionsSubScreen.list} is declared {@code @Nullable}, and rightly so — it does not exist
 * until {@code init()} runs, which is why vanilla's own {@code repositionElements} checks it. Inside
 * {@link #addOptions()} it always exists: {@code addContents} assigns the field, calls this, and
 * dereferences the field again on the next instruction without a check of its own. {@link
 * #optionsList()} states that guarantee once, where it can be read, instead of four screens each
 * asserting it or going without.
 */
@Environment(EnvType.CLIENT)
public abstract class ModOptionsScreen extends OptionsSubScreen {
    protected ModOptionsScreen(Screen lastScreen, Component title) {
        super(lastScreen, Minecraft.getInstance().options, title);
    }

    /** The list being filled in. Only meaningful from inside {@link #addOptions()}. */
    protected final OptionsList optionsList() {
        return Objects.requireNonNull(this.list, "The options list exists only once init() has run");
    }
}
