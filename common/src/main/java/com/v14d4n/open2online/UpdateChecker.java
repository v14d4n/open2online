package com.v14d4n.open2online;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
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
 * Replaces Forge's {@code VersionChecker}, which has no counterpart on Fabric. Reads the list the mod
 * publishes, in the format Forge's checker has always read, so NeoForge can be pointed at the same
 * file.
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
            "https://raw.githubusercontent.com/v14d4n/open2online/update/updatev2.json";
    private static final int HTTP_TIMEOUT_MS = 5_000;

    private static volatile MutableComponent notice;
    private static volatile boolean announced;
    /** The answer from {@link #UPDATE_URL}, so that every surface asking costs one request. */
    private static volatile String cachedLatestVersion;
    private static volatile String cachedDownloadPage;

    private UpdateChecker() {
    }

    /** Fires the lookup once, off the main thread. */
    public static void check(Minecraft minecraft) {
        // The switch covers the chat line, so with it off there is nothing this lookup could deliver.
        // Mod Menu asks separately and is not bound by it — that badge is its own notification.
        if (!OpenToOnlineConfig.updateNotifications.get()) {
            return;
        }

        Thread worker = new Thread(UpdateChecker::checkAndAnnounce, "Open2Online update check");
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

    private static void checkAndAnnounce() {
        String latest = lookUpLatestVersion();
        if (latest == null || latest.equals(installedVersion())) {
            return;
        }

        notice = buildNotice(installedVersion(), latest);

        // Already in a world by the time the answer arrived — say it now rather than next join.
        Minecraft.getInstance().execute(UpdateChecker::announceIfPending);
    }

    /**
     * The newest version published for the running Minecraft version, or {@code null} if the lookup
     * came back empty-handed. A non-null answer also means {@link #downloadPage()} now knows where to
     * send someone.
     *
     * <p>Blocking, so it belongs on a worker thread. Mod Menu asks the same question from a thread of
     * its own, hence the lock and the kept answer: whoever gets there first pays for the request.
     */
    public static synchronized String lookUpLatestVersion() {
        if (cachedLatestVersion != null) {
            return cachedLatestVersion;
        }

        String mcVersion = SharedConstants.getCurrentVersion().name();
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(UPDATE_URL).toURL().openConnection();
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);

            JsonObject root;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }

            JsonObject promos = root.getAsJsonObject("promos");
            if (promos == null) {
                return null;
            }

            // NeoForge reads the same file and only counts "-recommended" as a finished release.
            JsonElement published = promos.get(mcVersion + "-recommended");
            // Where a build should be downloaded from is the file's to say, so that it can move
            // without a release going out. NeoForge shows the same field.
            JsonElement homepage = root.get("homepage");
            if (published == null || homepage == null) {
                return null;
            }

            cachedDownloadPage = homepage.getAsString();
            cachedLatestVersion = published.getAsString();
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            LOGGER.warn("Update check failed", e);
            return null;
        }

        return cachedLatestVersion;
    }

    public static String installedVersion() {
        return Platform.getMod(OpenToOnline.MOD_ID).getVersion();
    }

    /** Where someone who wants the newer build should be sent, once the lookup has answered. */
    public static String downloadPage() {
        return cachedDownloadPage;
    }

    private static MutableComponent buildNotice(String current, String latest) {
        MutableComponent message = ModChatTranslatableComponent
                .of("chat.open2online.update", MessageTypes.WARN)
                .append(Component.literal(" "))
                .append(Component.literal(current).withStyle(ChatFormatting.RED))
                .append(Component.literal(" -> "))
                .append(Component.literal(latest).withStyle(ChatFormatting.GREEN));

        MutableComponent link = Component.translatable("chat.open2online.link").setStyle(Style.EMPTY
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(downloadPage())))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("tooltip.open2online.openUrl"))));

        return message.append(" [").append(link).append("]");
    }
}
