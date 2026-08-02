package com.v14d4n.open2online.screens;

import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.server.ModServerOptions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class EditWhitelistScreen extends ModOptionsScreen {
    private static final int ROW_WIDTH = 150;
    private static final int ROW_HEIGHT = 20;

    private String pendingName;

    private EditBox nameBox;
    private Button addButton;

    public EditWhitelistScreen(Screen lastScreen) {
        this(lastScreen, "");
    }

    private EditWhitelistScreen(Screen lastScreen, String pendingName) {
        super(lastScreen, Component.translatable("gui.open2online.editWhitelist"));
        this.pendingName = pendingName;
    }

    @Override
    protected void addOptions() {
        OptionsList optionsList = optionsList();

        // The whitelist toggle lives here, spanning the full width, as it did in 1.16.5.
        optionsList.addBig(ModServerOptions.whitelistMode());

        this.addButton = Button.builder(Component.literal("+"), press -> addFriend()).build();

        this.nameBox = new EditBox(this.font, 0, 0, ROW_WIDTH, ROW_HEIGHT, Component.empty());
        this.nameBox.setValue(this.pendingName);
        this.nameBox.setResponder(value -> {
            this.pendingName = value;
            updateAddButton();
        });

        optionsList.addSmall(this.nameBox, this.addButton);
        // The responder does not fire for the value set above, and a rebuild carries one over.
        updateAddButton();

        List<String> friends = OpenToOnlineConfig.friends.get();
        for (int i = 0; i < friends.size(); i++) {
            EditBox friendBox = new EditBox(this.font, 0, 0, ROW_WIDTH, ROW_HEIGHT, Component.empty());
            friendBox.setValue(friends.get(i));
            friendBox.setEditable(false);

            int index = i;
            Button removeButton = Button.builder(Component.literal("-"), press -> removeFriend(index)).build();
            optionsList.addSmall(friendBox, removeButton);
        }

        this.setInitialFocus(this.nameBox);
    }

    /**
     * Greys out "+" for anything pressing it would not achieve, and says why on the field itself —
     * the same shape {@code ShareToOnlineScreen} uses for a refused port.
     */
    private void updateAddButton() {
        String name = this.pendingName.trim();
        this.addButton.active = isAddable(name);
        this.nameBox.setTooltip(tooltipFor(name));
    }

    /** Null when there is nothing to say: the name is fine, or nothing has been typed yet. */
    private static Tooltip tooltipFor(String name) {
        if (name.isEmpty() || isAddable(name)) {
            return null;
        }

        return Tooltip.create(Component.translatable(isAlreadyListed(name)
                ? "gui.open2online.whitelistName.duplicate"
                : "gui.open2online.whitelistName.invalid"));
    }

    /**
     * Both halves of "pressing + would change nothing".
     *
     * <p>The first is vanilla's own rule, the one {@code ServerLoginPacketListenerImpl} applies to
     * the name in the login handshake: at most 16 characters, all of them printable ASCII. A name it
     * turns down is one no player can ever arrive under, so an entry holding one is a line the owner
     * believes in and the whitelist can never match. The emptiness test is ours — {@code
     * isValidPlayerName} accepts the empty string.
     */
    private static boolean isAddable(String name) {
        return !name.isEmpty() && StringUtil.isValidPlayerName(name) && !isAlreadyListed(name);
    }

    /** Ignoring case, because {@code ServerHandler.isWhitelisted} matches that way too. */
    private static boolean isAlreadyListed(String name) {
        return OpenToOnlineConfig.friends.get().stream().anyMatch(friend -> friend.equalsIgnoreCase(name));
    }

    private void addFriend() {
        String name = this.pendingName.trim();
        if (!isAddable(name)) {
            return;
        }

        ArrayList<String> updated = new ArrayList<>(OpenToOnlineConfig.friends.get());
        // Newest first, so a fresh entry shows up right under the input instead of at the far end of
        // a list the player then has to scroll to.
        updated.add(0, name);
        OpenToOnlineConfig.setFriends(updated);

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
