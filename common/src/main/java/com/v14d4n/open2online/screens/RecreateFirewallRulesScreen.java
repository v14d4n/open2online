package com.v14d4n.open2online.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.FormattedCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Windows-only helper: drops the stale Minecraft firewall rules and adds correct ones, for the case
 * where the server publishes without errors but nobody can reach it.
 */
@Environment(EnvType.CLIENT)
public class RecreateFirewallRulesScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("Open2Online");

    private static final int TEXT_WIDTH = 280;

    private static final Component WARNING = Component.translatable("gui.open2online.firewall.warning");

    /**
     * 1.16.5 hardcoded these as English string literals broken into fixed lines. They are
     * translatable now and wrapped by the font, so paragraphs flow one after another instead of
     * sitting at fixed offsets — otherwise a longer translation would overlap the next block.
     */
    private static final List<Component> EXPLANATION = List.of(
            Component.translatable("gui.open2online.firewall.whenToUse"),
            Component.translatable("gui.open2online.firewall.whatItDoes"),
            Component.translatable("gui.open2online.firewall.requirements"));

    private final Screen lastScreen;

    public RecreateFirewallRulesScreen(Screen lastScreen) {
        super(Component.translatable("gui.open2online.recreateFirewallRules"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button
                .builder(Component.translatable("gui.open2online.recreateRules"), press -> {
                    recreateFirewallRules();
                    this.minecraft.setScreen(this.lastScreen);
                })
                .bounds(this.width / 2 - 155, this.height / 4 + 132, 150, 20)
                .build());

        this.addRenderableWidget(Button
                .builder(CommonComponents.GUI_CANCEL, press -> this.minecraft.setScreen(this.lastScreen))
                .bounds(this.width / 2 + 5, this.height / 4 + 132, 150, 20)
                .build());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int x = this.width / 2 - 140;
        int y = this.height / 4;

        // Colours here are ARGB — a bare 0xFFFFFF has zero alpha and draws nothing. Use the same
        // constants vanilla screens use.
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2,
                Math.max(52, this.height / 4 - 8) - 20, CommonColors.WHITE);
        guiGraphics.drawString(this.font, WARNING, x, y, CommonColors.RED);

        int lineY = y + 2 * this.font.lineHeight;
        for (Component paragraph : EXPLANATION) {
            for (FormattedCharSequence line : this.font.split(paragraph, TEXT_WIDTH)) {
                guiGraphics.drawString(this.font, line, x, lineY, CommonColors.GRAY);
                lineY += this.font.lineHeight;
            }
            lineY += this.font.lineHeight;
        }
    }

    private void recreateFirewallRules() {
        Optional<String> path = getExecutablePath();
        if (path.isEmpty()) {
            LOGGER.error("Could not determine the Minecraft executable path; firewall rules were left alone.");
            return;
        }

        // Doubled apostrophes: the path lands inside a single-quoted PowerShell string, and that is
        // how one escapes a quote there. Rare, but "C:\Users\O'Brien\..." exists.
        String program = '"' + path.get().replace("'", "''") + '"';
        String commands = String.join(" && ",
                "netsh advfirewall firewall delete rule name=all program=" + program,
                "netsh advfirewall firewall add rule name=Minecraft_in dir=in action=allow program=" + program,
                "netsh advfirewall firewall add rule name=Minecraft_out dir=out action=allow program=" + program);
        String script = "Start-Process cmd -Verb RunAs -ArgumentList '/c " + commands + "'";

        try {
            // Two deliberate choices, both about getting the path through intact.
            //
            // ProcessBuilder rather than Runtime.exec(String): that one splits the whole command on
            // whitespace with a StringTokenizer that knows nothing about quotes, so a path under
            // "Program Files" arrives in pieces. Its own javadoc warns about exactly that, and it has
            // been deprecated since Java 18.
            //
            // -EncodedCommand rather than -Command: PowerShell parses the latter off the command line
            // before it is a string, and that parsing eats the quotes the path needs. Base64 of
            // UTF-16LE reaches the shell untouched. The old """ spelling existed to fight this.
            new ProcessBuilder("powershell", "-NoProfile", "-EncodedCommand",
                    Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE)))
                    .start();
        } catch (IOException e) {
            LOGGER.error("Failed to recreate firewall rules", e);
        }
    }

    /**
     * 1.16.5 shelled out to a generated VBScript and queried WMI for the running executable.
     * {@code ProcessHandle} has reported this directly since Java 9.
     */
    private static Optional<String> getExecutablePath() {
        return ProcessHandle.current().info().command();
    }
}
