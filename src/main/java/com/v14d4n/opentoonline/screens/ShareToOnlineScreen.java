package com.v14d4n.opentoonline.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.v14d4n.opentoonline.commands.OpenToOnlineCommand;
import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShareToOnlineScreen extends Screen {

    private static final Minecraft minecraft = Minecraft.getInstance();
    private static final Component ALLOW_COMMANDS_LABEL = new TranslatableComponent("selectWorld.allowCommands");
    private static final Component GAME_MODE_LABEL = new TranslatableComponent("selectWorld.gameMode");
    private static final Component SETTINGS_INFO_TEXT = new TranslatableComponent("lanServer.otherPlayers");
    private static final Component PORT_INFO_TEXT = new TranslatableComponent("gui.opentoonline.portInfo");
    private static final Component MAX_PLAYERS_INFO_TEXT = new TranslatableComponent("gui.opentoonline.maxPlayersInfo");
    private final Screen lastScreen;
    private GameType gameMode;
    private boolean commands;

    private Button openToOnlineButton;

    private EditBox portEditBox;
    private EditBox maxPlayersEditBox;

    private String initPort = OpenToOnlineConfig.port.get().toString();
    private String initMaxPlayers = OpenToOnlineConfig.maxPlayers.get().toString();

    public ShareToOnlineScreen(Screen pLastScreen) {
        super(new TranslatableComponent("gui.opentoonline.onlineWorld"));
        this.lastScreen = pLastScreen;
        this.gameMode = minecraft.gameMode.getPlayerMode();
        this.commands = (minecraft.player.getPermissionLevel() == 4);
    }

    @Override
    protected void init() {
        // GameMode button
        createGameModeButton();
        // Allow commands button
        createAllowCommandsButton();
        // Port edit box
        createPortEditBox();
        // Max players edit box
        createMaxPlayersEditBox();
        // Open to online button
        createOpenToOnlineButton();
        // Advanced settings button
        createAdvancedSettingsButton();
        // Fix firewall problem button
        createFixFirewallProblemButton();
        // Cancel button
        createCancelButton();
    }

    @Override
    public void tick() {
        this.portEditBox.tick();
        this.maxPlayersEditBox.tick();

        if (this.portEditBox.isFocused()) {
            this.maxPlayersEditBox.setFocus(false); // Fix menu bug
        }
    }

    private void createFixFirewallProblemButton() {
        this.addRenderableWidget(new Button(width / 2 - 155, height / 4 + 69, 150, 20, new TranslatableComponent("gui.opentoonline.recreateFirewallRules"), (p_96657_) -> {
            minecraft.setScreen(new RecreateFirewallRulesScreen(this));
        }));
    }

    private void createAdvancedSettingsButton() {
        this.addRenderableWidget(new Button(width / 2 + 5, height / 4 + 69, 150, 20, new TranslatableComponent("gui.opentoonline.advancedSettings"), (p_96657_) -> {
            minecraft.setScreen(new AdvancedSettingsScreen(this));
        }));
    }

    private void createCancelButton() {
        this.addRenderableWidget(new Button(width / 2 + 5, height - 28, 150, 20, CommonComponents.GUI_CANCEL, (p_96657_) -> {
            minecraft.setScreen(this.lastScreen);
        }));
    }

    private void createOpenToOnlineButton() {
        openToOnlineButton = this.addRenderableWidget(new Button(width / 2 - 155, height - 28, 150, 20, new TranslatableComponent("gui.opentoonline.startOnlineWorld"), (p_96660_) -> {
            minecraft.setScreen(null);
            int port = Integer.parseInt(portEditBox.getValue());
            int maxPlayers = Integer.parseInt(maxPlayersEditBox.getValue());
            new Thread(() -> OpenToOnlineCommand.open(port, maxPlayers, gameMode, commands)).start();
        }));
    }

    private void createPortEditBox() {
        portEditBox = new EditBox(this.font, width / 2 - 155 + 1, height / 4 + 45, 148, 20, new TextComponent(""));
        portEditBox.setValue(initPort);
        portEditBox.setResponder((pResponder) -> {
            this.initPort = pResponder;
            this.openToOnlineButton.active = isEditBoxesValuesValid();
        });
        this.addRenderableWidget(portEditBox);
    }

    private void createMaxPlayersEditBox() {
        maxPlayersEditBox = new EditBox(this.font, width / 2 + 5 + 1, height / 4 + 45, 148, 20, new TextComponent(""));
        maxPlayersEditBox.setValue(initMaxPlayers);
        maxPlayersEditBox.setResponder((pResponder) -> {
            this.initMaxPlayers = pResponder;
            this.openToOnlineButton.active = isEditBoxesValuesValid();
        });
        this.addRenderableWidget(maxPlayersEditBox);
    }

    private void createAllowCommandsButton() {
        this.addRenderableWidget(CycleButton.onOffBuilder(this.commands)
                .create(width / 2 + 5, height / 4 + 8, 150, 20, ALLOW_COMMANDS_LABEL, (p_169432_, p_169433_) -> {
            this.commands = p_169433_;
        }));
    }

    private void createGameModeButton() {
        this.addRenderableWidget(CycleButton.builder(GameType::getShortDisplayName).withValues(GameType.SURVIVAL, GameType.SPECTATOR, GameType.CREATIVE, GameType.ADVENTURE).withInitialValue(this.gameMode)
                .create(width / 2 - 155, height / 4 + 8, 150, 20, GAME_MODE_LABEL, (p_169429_, p_169430_) -> {
            this.gameMode = p_169430_;
        }));
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);
        drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, Math.max(52, height / 4 - 8) - 22, 16777215);
        drawCenteredString(pPoseStack, this.font, SETTINGS_INFO_TEXT, this.width / 2, Math.max(52, height / 4 - 8), 16777215);
        drawString(pPoseStack, this.font, PORT_INFO_TEXT, portEditBox.x, portEditBox.y - (portEditBox.getHeight() / 2) - 1, 16777215);
        drawString(pPoseStack, this.font, MAX_PLAYERS_INFO_TEXT, maxPlayersEditBox.x, maxPlayersEditBox.y - (maxPlayersEditBox.getHeight() / 2) - 1, 16777215);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    private boolean isEditBoxesValuesValid() {
        try {
            int port = Integer.parseInt(portEditBox.getValue());
            int maxPlayers = Integer.parseInt(maxPlayersEditBox.getValue());
            return !(port < 0 || port > 65535) && maxPlayers > 0;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
