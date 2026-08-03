package com.v14d4n.open2online;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.network.Http;
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
import net.minecraft.util.Util;

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
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);

    private static volatile MutableComponent notice;
    private static volatile boolean announced;
    /** The answer from {@link #UPDATE_URL}, so that every surface asking costs one request. */
    private static volatile String cachedLatestVersion;
    private static volatile String cachedDownloadPage;

    private UpdateChecker() {
    }

    /**
     * Fires the lookup once, off the main thread.
     */
    public static void check() {
        // The game's own pool for blocking I/O. It is shut down with the game, bounded to a three
        // second wait, so a lookup still in flight cannot hold the process open.
        Util.ioPool().execute(UpdateChecker::checkAndAnnounce);
    }

    /** Delivers the notice once there is a chat to deliver it to. */
    public static void announceIfPending() {
        MutableComponent pending = notice;
        if (pending == null || announced || !OpenToOnlineConfig.updateNotifications.get()) {
            return;
        }

        // Only in the player's own world. The 1.16.5 build got this for free by riding a server-side
        // login event, which never fired on a client connected to someone else's server; a guest has
        // no say in which version the host runs, so telling them is noise.
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.hasSingleplayerServer()) {
            return;
        }

        announced = true;
        ModChat.send(pending);
    }

    private static void checkAndAnnounce() {
        Optional<String> latest = lookUpLatestVersion()
                .filter(published -> !published.equals(installedVersion()));
        if (latest.isEmpty()) {
            return;
        }

        notice = buildNotice(installedVersion(), latest.get());

        // Already in a world by the time the answer arrived — say it now rather than next join.
        Minecraft.getInstance().execute(UpdateChecker::announceIfPending);
    }

    /**
     * The newest version published for the running Minecraft version, empty if the lookup came back
     * with nothing. An answer also means {@link #downloadPage()} now knows where to send someone.
     *
     * <p>Blocking, so it belongs on a worker thread. Mod Menu asks the same question from a thread of
     * its own, hence the lock and the kept answer: whoever gets there first pays for the request.
     */
    public static synchronized Optional<String> lookUpLatestVersion() {
        if (cachedLatestVersion != null) {
            return Optional.of(cachedLatestVersion);
        }

        Optional<String> body = Http.get(UPDATE_URL, HTTP_TIMEOUT);
        if (body.isEmpty()) {
            return Optional.empty();
        }

        String mcVersion = SharedConstants.getCurrentVersion().name();
        try {
            JsonObject root = JsonParser.parseString(body.get()).getAsJsonObject();
            JsonObject promos = root.getAsJsonObject("promos");
            if (promos == null) {
                return Optional.empty();
            }

            // NeoForge reads the same file and only counts "-recommended" as a finished release.
            JsonElement published = promos.get(mcVersion + "-recommended");
            // Where a build should be downloaded from is the file's to say, so that it can move
            // without a release going out. NeoForge shows the same field.
            JsonElement homepage = root.get("homepage");
            if (published == null || homepage == null) {
                return Optional.empty();
            }

            String latest = published.getAsString();

            cachedDownloadPage = Optional.ofNullable(root.getAsJsonObject("downloads"))
                    .map(downloads -> downloads.get(latest))
                    .map(JsonElement::getAsString)
                    .orElseGet(homepage::getAsString);

            // Set last: it is what the cache check above reads, so everything it implies has to be in
            // place before it becomes visible.
            cachedLatestVersion = latest;
        } catch (JsonParseException | IllegalStateException e) {
            // Transport failures are already logged and turned into an empty body above; what is left
            // to go wrong here is the file itself not being the shape this reads.
            LOGGER.warn("Update check failed", e);
            return Optional.empty();
        }

        return Optional.of(cachedLatestVersion);
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
