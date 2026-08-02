package com.v14d4n.open2online.screens;

import com.v14d4n.open2online.network.PublishTask;
import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.network.ServerHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Util;
import net.minecraft.world.level.GameType;

import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public class ShareToOnlineScreen extends Screen {
    private static final Component ALLOW_COMMANDS_LABEL = Component.translatable("selectWorld.allowCommands");
    private static final Component GAME_MODE_LABEL = Component.translatable("selectWorld.gameMode");
    private static final Component SETTINGS_INFO_TEXT = Component.translatable("lanServer.otherPlayers");
    private static final Component PORT_INFO_TEXT = Component.translatable("gui.open2online.portInfo");
    private static final Component MAX_PLAYERS_INFO_TEXT = Component.translatable("gui.open2online.maxPlayersInfo");

    private final Screen lastScreen;

    private GameType gameMode;
    private boolean commands;

    private Button onlineButton;
    private Button lanButton;

    private EditBox portEditBox;
    private EditBox maxPlayersEditBox;

    public ShareToOnlineScreen(Screen lastScreen) {
        super(Component.translatable("gui.open2online.onlineWorld"));
        this.lastScreen = lastScreen;
        // No null check on `minecraft`: since 1.21.11 the Screen constructor fills that field from
        // Minecraft.getInstance() rather than waiting for init(), so it is set by the time this runs.
        this.gameMode = this.minecraft.gameMode != null
                ? this.minecraft.gameMode.getPlayerMode()
                : GameType.SURVIVAL;
        this.commands = ServerHandler.canLocalPlayerUseCheats();
    }

    @Override
    protected void init() {
        createGameModeButton();
        createAllowCommandsButton();
        createPortEditBox();
        createMaxPlayersEditBox();
        createOpenToOnlineButton();
        createOpenToLanButton();
        createAdvancedSettingsButton();
        createRecreateFirewallRulesButton();
        createSupportDeveloperButton();
        createCancelButton();

        updateStartButtons();
    }

    /**
     * {@code CycleButton}, the same widget vanilla's {@code ShareToLanScreen} uses. The 1.16.5
     * screen cycled a plain {@code Button} by hand and repainted its label, but only because the
     * widget did not exist yet — the intended call was already sitting there commented out.
     */
    private void createGameModeButton() {
        this.addRenderableWidget(CycleButton
                .builder(GameType::getShortDisplayName, this.gameMode)
                .withValues(GameType.SURVIVAL, GameType.SPECTATOR, GameType.CREATIVE, GameType.ADVENTURE)
                .create(this.width / 2 - 155, this.height / 4 + 8, 150, 20, GAME_MODE_LABEL,
                        (button, value) -> this.gameMode = value));
    }

    private void createAllowCommandsButton() {
        this.addRenderableWidget(CycleButton
                .onOffBuilder(this.commands)
                .create(this.width / 2 + 5, this.height / 4 + 8, 150, 20, ALLOW_COMMANDS_LABEL,
                        (button, value) -> this.commands = value));
    }

    private void createPortEditBox() {
        this.portEditBox = new EditBox(this.font, this.width / 2 - 154, this.height / 4 + 45, 148, 20, Component.empty());
        this.portEditBox.setValue(OpenToOnlineConfig.port.get().toString());
        this.portEditBox.setResponder(value -> updateStartButtons());
        this.addRenderableWidget(this.portEditBox);
    }

    private void createMaxPlayersEditBox() {
        this.maxPlayersEditBox = new EditBox(this.font, this.width / 2 + 6, this.height / 4 + 45, 148, 20, Component.empty());
        this.maxPlayersEditBox.setValue(OpenToOnlineConfig.maxPlayers.get().toString());
        this.maxPlayersEditBox.setResponder(value -> updateStartButtons());
        this.addRenderableWidget(this.maxPlayersEditBox);
    }

    private void createOpenToOnlineButton() {
        this.onlineButton = this.addRenderableWidget(Button
                .builder(Component.translatable("gui.open2online.startOnlineWorld"), press -> startServer(true))
                .bounds(this.width / 2 - 155, this.height - 28, 150, 20)
                .build());
    }

    private void createOpenToLanButton() {
        this.lanButton = this.addRenderableWidget(Button
                .builder(Component.translatable("gui.open2online.startLanWorld"), press -> startServer(false))
                .bounds(this.width / 2 - 155, this.height - 51, 150, 20)
                .build());
    }

    /**
     * Port mapping does blocking network I/O, so it must not run on the render thread.
     */
    private void startServer(boolean online) {
        int port = parsePort().orElseThrow();
        int maxPlayers = parseMaxPlayers().orElseThrow();
        GameType mode = this.gameMode;
        boolean allowCommands = this.commands;

        this.minecraft.setScreen(null);
        Util.ioPool().execute(() -> PublishTask.start(port, maxPlayers, mode, allowCommands, online));
    }

    private void createAdvancedSettingsButton() {
        this.addRenderableWidget(Button
                .builder(Component.translatable("gui.open2online.advancedSettings"),
                        press -> this.minecraft.setScreen(new AdvancedSettingsScreen(this)))
                .bounds(this.width / 2 + 5, this.height / 4 + 69, 150, 20)
                .build());
    }

    private void createRecreateFirewallRulesButton() {
        this.addRenderableWidget(Button
                .builder(Component.translatable("gui.open2online.recreateFirewallRules"),
                        press -> this.minecraft.setScreen(new RecreateFirewallRulesScreen(this)))
                .bounds(this.width / 2 - 155, this.height / 4 + 69, 150, 20)
                .tooltip(Tooltip.create(Component.translatable("tooltip.open2online.recreateFirewallRules")))
                .build());
    }

    /** Sits in the free slot beside "Start LAN World"; only offered to Russian players. */
    private void createSupportDeveloperButton() {
        if (!SupportDeveloperPopup.isAvailable()) {
            return;
        }

        this.addRenderableWidget(Button
                .builder(Component.translatable("gui.open2online.supportDeveloper"),
                        press -> this.minecraft.setScreen(SupportDeveloperPopup.create(this)))
                .bounds(this.width / 2 + 5, this.height - 51, 150, 20)
                .build());
    }

    private void createCancelButton() {
        this.addRenderableWidget(Button
                .builder(CommonComponents.GUI_CANCEL, press -> this.minecraft.setScreen(this.lastScreen))
                .bounds(this.width / 2 + 5, this.height - 28, 150, 20)
                .build());
    }

    /**
     * Vanilla's {@code ShareToLanScreen} reports why a port is refused instead of just grey buttons
     * (it keeps {@code INVALID_PORT} / {@code PORT_UNAVAILABLE} for that), so the reason is surfaced
     * as a tooltip on the offending field.
     */
    private void updateStartButtons() {
        boolean portValid = parsePort().isPresent();
        boolean maxPlayersValid = parseMaxPlayers().isPresent();

        this.portEditBox.setTooltip(portValid
                ? null
                : Tooltip.create(Component.translatable("gui.open2online.port.invalid",
                        OpenToOnlineConfig.MIN_PORT, OpenToOnlineConfig.MAX_PORT)));
        this.maxPlayersEditBox.setTooltip(maxPlayersValid
                ? null
                : Tooltip.create(Component.translatable("gui.open2online.maxPlayers.invalid")));

        boolean valid = portValid && maxPlayersValid;
        this.onlineButton.active = valid;
        this.lanButton.active = valid;
    }

    private OptionalInt parsePort() {
        try {
            int port = Integer.parseInt(this.portEditBox.getValue().trim());
            return port >= OpenToOnlineConfig.MIN_PORT && port <= OpenToOnlineConfig.MAX_PORT
                    ? OptionalInt.of(port)
                    : OptionalInt.empty();
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    private OptionalInt parseMaxPlayers() {
        try {
            int maxPlayers = Integer.parseInt(this.maxPlayersEditBox.getValue().trim());
            return maxPlayers > 0 ? OptionalInt.of(maxPlayers) : OptionalInt.empty();
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Colours here are ARGB — a bare 0xFFFFFF has zero alpha and draws nothing. Use the same
        // constants vanilla screens use.
        int headerY = Math.max(52, this.height / 4 - 8);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, headerY - 22, CommonColors.WHITE);
        guiGraphics.drawCenteredString(this.font, SETTINGS_INFO_TEXT, this.width / 2, headerY, CommonColors.WHITE);
        guiGraphics.drawString(this.font, PORT_INFO_TEXT,
                this.portEditBox.getX(), this.portEditBox.getY() - 12, CommonColors.WHITE);
        guiGraphics.drawString(this.font, MAX_PLAYERS_INFO_TEXT,
                this.maxPlayersEditBox.getX(), this.maxPlayersEditBox.getY() - 12, CommonColors.WHITE);
    }

}
