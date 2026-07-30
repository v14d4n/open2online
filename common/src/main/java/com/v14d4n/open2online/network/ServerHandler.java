package com.v14d4n.open2online.network;

import com.mojang.authlib.GameProfile;
import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.network.chat.ModChat;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class ServerHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Open2Online");

    /** Two independent echo services, in case one is blocked or down. */
    private static final List<String> EXTERNAL_IP_SERVICES = List.of(
            "https://checkip.amazonaws.com",
            "https://api.ipify.org");
    private static final int ATTEMPTS_PER_SERVICE = 2;
    private static final long RETRY_BACKOFF_MS = 500L;
    private static final int HTTP_TIMEOUT_MS = 3_000;

    /** Placeholder the config ships with, meaning "no address has ever been resolved". */
    private static final String DEFAULT_IP = "0.0.0.0";
    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    private ServerHandler() {
    }

    public static boolean startServer(int port, int maxPlayers, GameType gameMode, boolean allowCommands, boolean online) {
        if (online && !UPnPHandler.openPort(port)) {
            return false;
        }

        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return false;
        }

        // Must land before publishing: usesAuthentication is read when a client connects, to fill in
        // the shouldAuthenticate flag of the login handshake.
        applyLicenceRequirement(server, online);

        if (server.publishServer(gameMode, allowCommands, port)) {
            setupAndSaveServerConfiguration(maxPlayers, port, online);
            printHostedGameMessage(online, port);
        } else {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.error.publishFailed",
                    ModChatTranslatableComponent.MessageTypes.ERROR));
            UPnPHandler.closePort(port);
            return false;
        }

        return true;
    }

    private static void printHostedGameMessage(boolean online, int port) {
        if (!online) {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.localGameHostedOn")
                    .append(getServerFormattedPort(port)));
            return;
        }

        String address = resolveExternalIP() + ":" + port;
        MutableComponent shown = OpenToOnlineConfig.hideIP.get()
                ? bracketed(copyable(Component.translatable("tooltip.open2online.copy").getString(), address))
                : bracketed(copyable(address, address));
        ModChat.send(ModChatTranslatableComponent.of("chat.open2online.gameHostedOn").append(shown));
    }

    private static void setupAndSaveServerConfiguration(int maxPlayers, int port, boolean closePortAfterLogout) {
        setPvpAllowed(OpenToOnlineConfig.allowPvp.get());

        // Max players is applied by MixinIntegratedServer reading the config, so there is nothing
        // to push into the running server here — only the stored value needs updating.
        OpenToOnlineConfig.maxPlayers.set(maxPlayers);
        OpenToOnlineConfig.port.set(port);
        OpenToOnlineConfig.maxPlayers.save();
        UPnPHandler.closePortAfterLogout(closePortAfterLogout);
    }

    public static boolean isServerPublished() {
        Minecraft minecraft = Minecraft.getInstance();
        return !minecraft.hasSingleplayerServer() || minecraft.getSingleplayerServer().isPublished();
    }

    /**
     * Since 1.20.5 the server tells the client whether to verify its session, through the
     * {@code shouldAuthenticate} flag of {@code ClientboundHelloPacket}, and that flag comes from
     * {@code usesAuthentication}. Turning it off is what lets players join when session verification
     * cannot succeed — at the cost of anyone being able to claim any name, hence the warning.
     */
    private static void applyLicenceRequirement(IntegratedServer server, boolean online) {
        boolean requireLicense = OpenToOnlineConfig.requireLicense.get();
        server.setUsesAuthentication(requireLicense);

        if (online && !requireLicense) {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.warn.licenseNotRequired",
                    ModChatTranslatableComponent.MessageTypes.WARN));
        }
    }

    /**
     * PvP used to be a server flag toggled through {@code MinecraftServer.setPvpAllowed}. That
     * method is gone in 1.21.11 and PvP is a game rule now, so this writes the rule instead — which
     * means the setting is persisted into the world rather than living only for the session.
     */
    private static void setPvpAllowed(boolean allowPvp) {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return;
        }
        // Called from the publish worker; game rule writes belong on the server thread.
        server.execute(() -> server.overworld().getGameRules().set(GameRules.PVP, allowPvp, server));
    }

    /**
     * Resolves the address to advertise: a fresh lookup, else the last one that worked, else the
     * {@link #DEFAULT_IP} placeholder. Never fails — the server is already published by this point,
     * so the message has to go out either way, as it did on 1.16.5.
     */
    private static String resolveExternalIP() {
        Optional<String> fetched = fetchExternalIP();
        String lastIP = OpenToOnlineConfig.lastIP.get();

        if (fetched.isPresent()) {
            String currentIP = fetched.get();
            if (!currentIP.equals(lastIP)) {
                if (!lastIP.equals(DEFAULT_IP)) {
                    ModChat.send(ModChatTranslatableComponent.of("chat.open2online.ipIsChanged",
                            ModChatTranslatableComponent.MessageTypes.WARN));
                }
                OpenToOnlineConfig.lastIP.set(currentIP);
                OpenToOnlineConfig.lastIP.save();
            }
            return currentIP;
        }

        ModChat.send(ModChatTranslatableComponent.of("chat.open2online.warn.gettingAnExternalIP",
                ModChatTranslatableComponent.MessageTypes.WARN));

        if (!lastIP.equals(DEFAULT_IP)) {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.warn.usingLastKnownIP",
                    ModChatTranslatableComponent.MessageTypes.WARN));
            return lastIP;
        }

        return DEFAULT_IP;
    }

    /** Walks both services, twice each, with a small backoff between rounds. */
    private static Optional<String> fetchExternalIP() {
        for (int round = 1; round <= ATTEMPTS_PER_SERVICE; round++) {
            for (String service : EXTERNAL_IP_SERVICES) {
                Optional<String> ip = queryExternalIP(service);
                if (ip.isPresent()) {
                    return ip;
                }
            }

            if (round < ATTEMPTS_PER_SERVICE) {
                try {
                    Thread.sleep(RETRY_BACKOFF_MS * round);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> queryExternalIP(String service) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(service).toURL().openConnection();
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);

            String body;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                body = reader.readLine();
            }

            if (body == null) {
                return Optional.empty();
            }

            // A blocked or misbehaving service can answer 200 with an HTML notice, so the shape of
            // the reply is checked rather than trusted.
            String candidate = body.trim();
            return IPV4.matcher(candidate).matches() ? Optional.of(candidate) : Optional.empty();
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("External IP lookup via {} failed", service, e);
            return Optional.empty();
        }
    }

    private static MutableComponent getServerFormattedPort(int port) {
        return bracketed(copyable(String.valueOf(port), String.valueOf(port)));
    }

    private static MutableComponent copyable(String label, String valueToCopy) {
        return Component.literal(label).setStyle(Style.EMPTY
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.CopyToClipboard(valueToCopy))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("tooltip.open2online.copy"))));
    }

    private static MutableComponent bracketed(Component inner) {
        return Component.literal(" [").append(inner).append("]");
    }

    /**
     * 1.16.5 asked {@code player.hasPermissions(4)}. Permissions were rebuilt in 1.21.11 around
     * {@code PermissionSet}, and the old numeric level 4 is now {@code PermissionLevel.OWNERS}.
     */
    public static boolean canLocalPlayerUseCheats() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null
                && player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.OWNERS));
    }

    public static boolean isPlayerServerOwner(GameProfile gameProfile) {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        return server != null && server.isSingleplayerOwner(new NameAndId(gameProfile));
    }

    public static boolean isClientRunningOnlineServer() {
        return UPnPHandler.getClosePortAfterLogout();
    }
}
