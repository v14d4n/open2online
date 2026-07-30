package com.v14d4n.open2online;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.network.chat.ModChat;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent.MessageTypes;
import dev.architectury.platform.Platform;
import net.minecraft.ChatFormatting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * Replaces Forge's {@code VersionChecker}, which has no counterpart on Fabric. Reads the same
 * {@code update.json} the mod already publishes, so the file format does not change.
 */
@Environment(EnvType.CLIENT)
public final class UpdateChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger("Open2Online");
    private static final String UPDATE_URL =
            "https://raw.githubusercontent.com/v14d4n/open2online/update/update.json";
    private static final String HOMEPAGE =
            "https://www.curseforge.com/minecraft/mc-mods/open2online/files";
    private static final int HTTP_TIMEOUT_MS = 5_000;

    private static boolean alreadyChecked;

    private UpdateChecker() {
    }

    /** Runs once per session, off the main thread — this does blocking network I/O. */
    public static void checkOnce() {
        if (alreadyChecked || !OpenToOnlineConfig.updateNotifications.get()) {
            return;
        }
        alreadyChecked = true;

        Thread worker = new Thread(UpdateChecker::check, "Open2Online update check");
        worker.setDaemon(true);
        worker.start();
    }

    private static void check() {
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
        } catch (Exception e) {
            LOGGER.warn("Update check failed", e);
            return;
        }

        String latestVersion = stripMinecraftPrefix(latest);
        if (latestVersion.equals(stripMinecraftPrefix(currentVersion))) {
            return;
        }

        announce(stripMinecraftPrefix(currentVersion), latestVersion);
    }

    /** {@code update.json} stores entries as {@code <mcVersion>-<modVersion>}. */
    private static String stripMinecraftPrefix(String version) {
        int separator = version.lastIndexOf('-');
        return separator < 0 ? version : version.substring(separator + 1);
    }

    private static void announce(String current, String latest) {
        MutableComponent message = ModChatTranslatableComponent
                .of("chat.open2online.update", MessageTypes.WARN)
                .append(Component.literal(" "))
                .append(Component.literal(current).withStyle(ChatFormatting.RED))
                .append(Component.literal(" -> "))
                .append(Component.literal(latest).withStyle(ChatFormatting.GREEN));

        MutableComponent link = Component.translatable("chat.open2online.link").setStyle(Style.EMPTY
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(HOMEPAGE)))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("tooltip.open2online.openUrl"))));

        // ModChat.send already hops to the render thread.
        ModChat.send(message.append(" [").append(link).append("]"));
    }
}
