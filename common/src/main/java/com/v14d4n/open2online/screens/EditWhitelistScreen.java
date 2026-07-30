package com.v14d4n.open2online.screens;

import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.server.ModServerOptions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class EditWhitelistScreen extends OptionsSubScreen {
    private static final int ROW_WIDTH = 150;
    private static final int ROW_HEIGHT = 20;

    private String pendingName;

    public EditWhitelistScreen(Screen lastScreen) {
        this(lastScreen, "");
    }

    private EditWhitelistScreen(Screen lastScreen, String pendingName) {
        super(lastScreen, Minecraft.getInstance().options,
                Component.translatable("gui.open2online.editWhitelist"));
        this.pendingName = pendingName;
    }

    @Override
    protected void addOptions() {
        // The whitelist toggle lives here, spanning the full width, as it did in 1.16.5.
        this.list.addBig(ModServerOptions.whitelistMode());

        EditBox nameBox = new EditBox(this.font, 0, 0, ROW_WIDTH, ROW_HEIGHT, Component.empty());
        nameBox.setValue(this.pendingName);
        nameBox.setResponder(value -> this.pendingName = value);

        Button addButton = Button.builder(Component.literal("+"), press -> addFriend()).build();
        this.list.addSmall(nameBox, addButton);

        List<String> friends = OpenToOnlineConfig.friends.get();
        for (int i = 0; i < friends.size(); i++) {
            EditBox friendBox = new EditBox(this.font, 0, 0, ROW_WIDTH, ROW_HEIGHT, Component.empty());
            friendBox.setValue(friends.get(i));
            friendBox.setEditable(false);

            int index = i;
            Button removeButton = Button.builder(Component.literal("-"), press -> removeFriend(index)).build();
            this.list.addSmall(friendBox, removeButton);
        }

        this.setInitialFocus(nameBox);
    }

    private void addFriend() {
        String name = this.pendingName.trim();
        if (name.isEmpty()) {
            return;
        }

        ArrayList<String> updated = new ArrayList<>(OpenToOnlineConfig.friends.get());
        if (!updated.contains(name)) {
            // Newest first, so a fresh entry shows up right under the input instead of at the far
            // end of a list the player then has to scroll to.
            updated.add(0, name);
            OpenToOnlineConfig.setFriends(updated);
        }

        this.pendingName = "";
        rebuild();
    }

    private void removeFriend(int index) {
        ArrayList<String> updated = new ArrayList<>(OpenToOnlineConfig.friends.get());
        if (index >= 0 && index < updated.size()) {
            updated.remove(index);
            OpenToOnlineConfig.setFriends(updated);
        }
        rebuild();
    }

    /**
     * The row count changes with the list and {@code OptionsList} is built once in {@code init}, so
     * the screen is pushed anew — carrying whatever is typed in the input.
     *
     * <p>{@code rebuildWidgets()} cannot be used here: {@code OptionsSubScreen} builds its
     * {@code HeaderAndFooterLayout} in the constructor and that class exposes only
     * {@code addToHeader}/{@code addToContents}/{@code addToFooter} with no way to clear it. A second
     * {@code init()} therefore stacks another title, list and footer into the same frames, which
     * renders the screen twice over itself and leaves the inputs unreachable.
     */
    private void rebuild() {
        this.minecraft.setScreen(new EditWhitelistScreen(this.lastScreen, this.pendingName));
    }
}
