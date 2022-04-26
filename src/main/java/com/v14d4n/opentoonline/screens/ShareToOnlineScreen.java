package com.v14d4n.opentoonline.screens;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.v14d4n.opentoonline.commands.OpenToOnlineCommand;
import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ShareToLanScreen;
import net.minecraft.client.gui.screen.VideoSettingsScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.reflect.Field;

import static com.v14d4n.opentoonline.OpenToOnline.minecraft;

@OnlyIn(Dist.CLIENT)
public class ShareToOnlineScreen extends Screen {

    private static final Minecraft minecraft = Minecraft.getInstance();
    private static final TextComponent ALLOW_COMMANDS_LABEL = new TranslationTextComponent("selectWorld.allowCommands");
    private static final TextComponent GAME_MODE_LABEL = new TranslationTextComponent("selectWorld.gameMode");
    private static final TextComponent SETTINGS_INFO_TEXT = new TranslationTextComponent("lanServer.otherPlayers");
    private static final TextComponent PORT_INFO_TEXT = new TranslationTextComponent("gui.opentoonline.portInfo");
    private static final TextComponent MAX_PLAYERS_INFO_TEXT = new TranslationTextComponent("gui.opentoonline.maxPlayersInfo");
    private final Screen lastScreen;
    private GameType gameMode;
    private boolean commands;

    private Button openToOnlineButton;
    private Button commandsButton;
    private Button modeButton;

    private TextFieldWidget portEditBox;
    private TextFieldWidget maxPlayersEditBox;

    private String initPort = OpenToOnlineConfig.port.get().toString();
    private String initMaxPlayers = OpenToOnlineConfig.maxPlayers.get().toString();

    public ShareToOnlineScreen(Screen pLastScreen) {
        super(new TranslationTextComponent("gui.opentoonline.onlineWorld"));
        this.lastScreen = pLastScreen;
        this.gameMode = minecraft.gameMode.getPlayerMode();
        this.commands = minecraft.player.hasPermissions(4);
    }

    @Override
    protected void init() {
//        // GameMode button
        createGameModeButton();
//        // Allow commands button
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
        createRecreateFirewallRulesButton();
        // Cancel button
        createCancelButton();

        this.updateSelectionStrings();
    }

    @Override
    public void tick() {
        this.portEditBox.tick();
        this.maxPlayersEditBox.tick();

        if (this.portEditBox.isFocused()) {
            this.maxPlayersEditBox.setFocus(false); // Fix menu bug
        }
    }

    private void createRecreateFirewallRulesButton() {
        this.addButton(new Button(width / 2 - 155, height / 4 + 69, 150, 20, new TranslationTextComponent("gui.opentoonline.recreateFirewallRules"),
                (p_96657_) -> minecraft.setScreen(new RecreateFirewallRulesScreen(this)),
                (pButton, pPoseStack, pMouseX, pMouseY) -> this.renderTooltip(pPoseStack, minecraft.font.split(new TranslationTextComponent("tooltip.opentoonline.recreateFirewallRules"), 200), pMouseX, pMouseY)
        ));
    }

    private void createAdvancedSettingsButton() {
        this.addButton(new Button(width / 2 + 5, height / 4 + 69, 150, 20, new TranslationTextComponent("gui.opentoonline.advancedSettings"), (p_213094_1_) -> {
            minecraft.setScreen(new AdvancedSettingsScreen(this));
        }));

    }

    private void createCancelButton() {
        this.addButton(new Button(width / 2 + 5, height - 28, 150, 20, DialogTexts.GUI_CANCEL, (p_213094_1_) -> {
            minecraft.setScreen(this.lastScreen);
        }));
    }

    private void createOpenToOnlineButton() {
        openToOnlineButton = this.addButton(new Button(width / 2 - 155, height - 28, 150, 20, new TranslationTextComponent("gui.opentoonline.startOnlineWorld"), (p_213094_1_) -> {
            minecraft.setScreen(null);
            int port = Integer.parseInt(portEditBox.getValue());
            int maxPlayers = Integer.parseInt(maxPlayersEditBox.getValue());
            new Thread(() -> OpenToOnlineCommand.open(port, maxPlayers, gameMode, commands)).start();
        }));
    }

    private void createPortEditBox() {
        portEditBox = new TextFieldWidget(this.font, width / 2 - 155 + 1, height / 4 + 45, 148, 20, new StringTextComponent(""));
        portEditBox.setValue(initPort);
        portEditBox.setResponder((pResponder) -> {
            this.initPort = pResponder;
            this.openToOnlineButton.active = isEditBoxesValuesValid();
        });
        this.addWidget(portEditBox);
    }

    private void createMaxPlayersEditBox() {
        maxPlayersEditBox = new TextFieldWidget(this.font, width / 2 + 5 + 1, height / 4 + 45, 148, 20, new StringTextComponent(""));
        maxPlayersEditBox.setValue(initMaxPlayers);
        maxPlayersEditBox.setResponder((pResponder) -> {
            this.initMaxPlayers = pResponder;
            this.openToOnlineButton.active = isEditBoxesValuesValid();
        });
        this.addWidget(maxPlayersEditBox);
    }

    private void createAllowCommandsButton() {
        commandsButton = this.addButton(new Button(width / 2 + 5, height / 4 + 8, 150, 20, ALLOW_COMMANDS_LABEL, (p_213083_1_) -> {
            this.commands = !this.commands;
            this.updateSelectionStrings();
        }));
    }

    private void createGameModeButton() {
//        this.addWidget(CycleButton.builder(GameType::getShortDisplayName).withValues(GameType.SURVIVAL, GameType.SPECTATOR, GameType.CREATIVE, GameType.ADVENTURE).withInitialValue(this.gameMode)
//                .create(width / 2 - 155, height / 4 + 8, 150, 20, GAME_MODE_LABEL, (p_169429_, p_169430_) -> {
//            this.gameMode = p_169430_;
//        }));

        this.modeButton = this.addButton(new Button(width / 2 - 155, height / 4 + 8, 150, 20, GAME_MODE_LABEL, (p_213084_1_) -> {
            if (GameType.SPECTATOR.equals(this.gameMode)) {
                this.gameMode = GameType.CREATIVE;
            } else if (GameType.CREATIVE.equals(this.gameMode)) {
                this.gameMode = GameType.ADVENTURE;
            } else if (GameType.ADVENTURE.equals(this.gameMode)) {
                this.gameMode = GameType.SURVIVAL;
            } else {
                this.gameMode = GameType.SPECTATOR;
            }

            this.updateSelectionStrings();
        }));
    }

    private void updateSelectionStrings() {
        this.modeButton.setMessage(new TranslationTextComponent("options.generic_value", GAME_MODE_LABEL, new TranslationTextComponent("selectWorld.gameMode." + this.gameMode.getName())));
        this.commandsButton.setMessage(DialogTexts.optionStatus(ALLOW_COMMANDS_LABEL, this.commands));
    }

    @Override
    public void render(MatrixStack pPoseStack, int pMouseX, int pMouseY, float pPartialTicks) {
        this.renderBackground(pPoseStack);
        this.portEditBox.render(pPoseStack, pMouseX, pMouseY, pPartialTicks);
        this.maxPlayersEditBox.render(pPoseStack, pMouseX, pMouseY, pPartialTicks);
        drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, Math.max(52, height / 4 - 8) - 22, 16777215);
        drawCenteredString(pPoseStack, this.font, SETTINGS_INFO_TEXT, this.width / 2, Math.max(52, height / 4 - 8), 16777215);
        drawString(pPoseStack, this.font, PORT_INFO_TEXT, portEditBox.x, portEditBox.y - (portEditBox.getHeight() / 2) - 1, 16777215);
        drawString(pPoseStack, this.font, MAX_PLAYERS_INFO_TEXT, maxPlayersEditBox.x, maxPlayersEditBox.y - (maxPlayersEditBox.getHeight() / 2) - 1, 16777215);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTicks);
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
