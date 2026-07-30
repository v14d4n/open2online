package com.v14d4n.open2online;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.network.chat.ModChat;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent.MessageTypes;

import dev.architectury.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Replaces Forge's {@code VersionChecker}, which has no counterpart on Fabric. Reads the same
 * {@code update.json} the mod already publishes, so the file format does not change.
 *
 * <p>The lookup runs at client start and the answer is parked until the player is somewhere it can be
 * read. Doing both at world entry meant the notice landed a second or two after everything else the
 * mod says, because it had to wait for the network first; there is no chat at the title screen, so
 * the two steps have to be separate.
 */
@Environment(EnvType.CLIENT)
public final class UpdateChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger("Open2Online");
    private static final String UPDATE_URL =
            "https://raw.githubusercontent.com/v14d4n/open2online/update/update.json";
    private static final String HOMEPAGE =
            "https://modrinth.com/project/open2online/versions";
    private static final int HTTP_TIMEOUT_MS = 5_000;

    private static volatile MutableComponent notice;
    private static volatile boolean announced;

    private UpdateChecker() {
    }

    /** Fires the lookup once, off the main thread. */
    public static void check(Minecraft minecraft) {
        // Nothing is fetched when the notice is switched off, so the mod stays quiet on the network too.
        if (!OpenToOnlineConfig.updateNotifications.get()) {
            return;
        }

        Thread worker = new Thread(UpdateChecker::lookUpLatestVersion, "Open2Online update check");
        worker.setDaemon(true);
        worker.start();
    }

    /** Delivers the notice once there is a chat to deliver it to. */
    public static void announceIfPending() {
        MutableComponent pending = notice;
        if (pending == null || announced || !OpenToOnlineConfig.updateNotifications.get()) {
            return;
        }
        if (Minecraft.getInstance().player == null) {
            return;
        }

        announced = true;
        ModChat.send(pending);
    }

    private static void lookUpLatestVersion() {
        String currentVersion = Platform.getMod(OpenToOnline.MOD_ID).getVersion();
        String key = SharedConstants.getCurrentVersion().name() + "-latest";

        String latest;
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(UPDATE_URL).toURL().openConnection();
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);

            JsonObject root;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }

            JsonObject promos = root.getAsJsonObject("promos");
            if (promos == null || !promos.has(key)) {
                return;
            }
            latest = promos.get(key).getAsString();
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            LOGGER.warn("Update check failed", e);
            return;
        }

        String latestVersion = stripMinecraftPrefix(latest);
        String installedVersion = stripMinecraftPrefix(currentVersion);
        if (latestVersion.equals(installedVersion)) {
            return;
        }

        notice = buildNotice(installedVersion, latestVersion);

        // Already in a world by the time the answer arrived — say it now rather than next join.
        Minecraft.getInstance().execute(UpdateChecker::announceIfPending);
    }

    /** {@code update.json} stores entries as {@code <mcVersion>-<modVersion>}. */
    private static String stripMinecraftPrefix(String version) {
        int separator = version.lastIndexOf('-');
        return separator < 0 ? version : version.substring(separator + 1);
    }

    private static MutableComponent buildNotice(String current, String latest) {
        String homepage = Platform.getMod(OpenToOnline.MOD_ID)
                .getHomepage()
                .orElse(HOMEPAGE);

        MutableComponent message = ModChatTranslatableComponent
                .of("chat.open2online.update", MessageTypes.WARN)
                .append(Component.literal(" "))
                .append(Component.literal(current).withStyle(ChatFormatting.RED))
                .append(Component.literal(" -> "))
                .append(Component.literal(latest).withStyle(ChatFormatting.GREEN));

        MutableComponent link = Component.translatable("chat.open2online.link").setStyle(Style.EMPTY
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(homepage)))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("tooltip.open2online.openUrl"))));

        return message.append(" [").append(link).append("]");
    }
}
