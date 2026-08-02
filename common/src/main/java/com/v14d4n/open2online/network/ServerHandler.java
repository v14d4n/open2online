package com.v14d4n.open2online.network;

import com.google.common.net.InetAddresses;
import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.mixin.MinecraftTitleInvoker;
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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Optional;

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

    private ServerHandler() {
    }

    public static void startServer(int port, int maxPlayers, GameType gameMode, boolean allowCommands, boolean online) {
        if (online && !UPnPHandler.openPort(port)) {
            return;
        }

        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();

        // The world can be left while the port is still being mapped; the mapping must not outlive it.
        if (server == null) {
            if (online) {
                UPnPHandler.closePort(port);
            }
            return;
        }

        // Must land before publishing: usesAuthentication is read when a client connects, to fill in
        // the shouldAuthenticate flag of the login handshake.
        applyLicenceRequirement(server);

        if (!server.publishServer(gameMode, allowCommands, port)) {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.error.publishFailed",
                    ModChatTranslatableComponent.MessageTypes.ERROR));
            // Same guard as the branch above: only the online path ever mapped anything, and
            // closePort throws when no backend was ever chosen.
            if (online) {
                UPnPHandler.closePort(port);
            }
            return;
        }

        setupAndSaveServerConfiguration(maxPlayers, port, online);
        // Before the address, so the thing the host actually needs stays the last line in chat.
        warnAboutOpenAccess(online);
        printHostedGameMessage(online, port);
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
        refreshWindowTitle();
    }

    /**
     * Repaints the window title. Publishing does not make vanilla rebuild it, so the marker added by
     * {@code MixinMinecraft} has to be pushed out explicitly — otherwise it appears only once some
     * other event happens to refresh the title.
     */
    public static void refreshWindowTitle() {
        Minecraft minecraft = Minecraft.getInstance();
        // Setting the window title goes through GLFW, which belongs on the main thread; publishing
        // runs on a worker.
        minecraft.execute(() -> ((MinecraftTitleInvoker) minecraft).open2online$updateTitle());
    }

    /** Read once rather than asked twice: the world can be left between two calls, from this thread. */
    public static boolean isServerPublished() {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        return server == null || server.isPublished();
    }

    /**
     * Since 1.20.5 the server tells the client whether to verify its session, through the
     * {@code shouldAuthenticate} flag of {@code ClientboundHelloPacket}, and that flag comes from
     * {@code usesAuthentication}. Turning it off is what lets players join when session verification
     * cannot succeed — at the cost of anyone being able to claim any name, hence the warning.
     */
    private static void applyLicenceRequirement(IntegratedServer server) {
        server.setUsesAuthentication(OpenToOnlineConfig.requireLicense.get());
    }

    /**
     * Points out the two settings that leave a published server open to strangers. Each can be
     * silenced separately on the notifications screen.
     */
    private static void warnAboutOpenAccess(boolean online) {
        if (!online) {
            return;
        }

        if (!OpenToOnlineConfig.requireLicense.get() && OpenToOnlineConfig.licenseNotifications.get()) {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.warn.licenseNotRequired",
                    ModChatTranslatableComponent.MessageTypes.WARN));
        }

        if (!OpenToOnlineConfig.whitelistMode.get() && OpenToOnlineConfig.whitelistNotifications.get()) {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.warn.whitelistDisabled",
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
     * Resolves the address to advertise: the router first, then an echo service, else the last one
     * that worked, else the {@link #DEFAULT_IP} placeholder. Never fails — the server is already
     * published by this point, so the message has to go out either way, as it did on 1.16.5.
     */
    private static String resolveExternalIP() {
        Optional<String> fromRouter = askRouter();
        Optional<String> fetched = fromRouter.or(ServerHandler::fetchExternalIP);
        String lastIP = OpenToOnlineConfig.lastIP.get();

        if (fetched.isPresent()) {
            // Only worth saying when there is an address to qualify. The paths below already warn,
            // and more loudly, about one that is stale or missing altogether.
            if (fromRouter.isEmpty()) {
                ModChat.send(ModChatTranslatableComponent.of("chat.open2online.warn.routerIPUnknown",
                        ModChatTranslatableComponent.MessageTypes.WARN));
            }

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

    /**
     * The address the port mapping protocol already reported. Costs nothing — the backend learned it
     * during the exchange that opened the port — and it answers even where the echo services are
     * unreachable.
     *
     * <p>Checked rather than trusted: UPnP-IGD and NAT-PMP report the router's own WAN address, and
     * behind carrier-grade NAT or a second router that address belongs to the carrier or to a private
     * range, so nobody outside could dial it. Only PCP is told the address the mapping actually got.
     */
    private static Optional<String> askRouter() {
        return UPnPHandler.externalAddress().filter(ServerHandler::isReachableFromOutside);
    }

    private static boolean isReachableFromOutside(String address) {
        return parseIPv4(address).filter(ServerHandler::isPublic).isPresent();
    }

    /**
     * Parses a literal without ever consulting DNS — which is the whole reason this does not use
     * {@code InetAddress.getByName}, since that treats anything it cannot parse as a hostname and
     * goes asking a name server about it.
     *
     * <p>Narrowed to IPv4 on purpose: Guava accepts IPv6 literals too, and the checks below read
     * octets by position.
     */
    private static Optional<Inet4Address> parseIPv4(String address) {
        if (!InetAddresses.isInetAddress(address)) {
            return Optional.empty();
        }

        return InetAddresses.forString(address) instanceof Inet4Address parsed
                ? Optional.of(parsed)
                : Optional.empty();
    }

    private static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }

        // 100.64.0.0/10, the range carriers keep for their own layer of NAT. Java has no test for it,
        // and it is exactly the case a router reports while being unreachable from the outside.
        byte[] octets = address.getAddress();
        int second = octets[1] & 0xFF;
        return !((octets[0] & 0xFF) == 100 && second >= 64 && second <= 127);
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
            return parseIPv4(candidate).isPresent() ? Optional.of(candidate) : Optional.empty();
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

    /**
     * Vanilla's own answer to "is this the person hosting". It compares names, case-insensitively —
     * which is as far as anyone can get: with licence verification off a player picks their own name,
     * and the offline UUID is derived from that name, so there is no identity underneath to check.
     */
    public static boolean isPlayerServerOwner(NameAndId nameAndId) {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        return server != null && server.isSingleplayerOwner(nameAndId);
    }

    public static boolean isClientRunningOnlineServer() {
        return UPnPHandler.getClosePortAfterLogout();
    }

    /**
     * Membership test behind the whitelist, consulted by {@code MixinPlayerList} from vanilla's own
     * login gate. Matching is by name because the config stores names: the native whitelist keys its
     * entries by UUID, which would mean resolving every nickname through Mojang's profile API.
     */
    public static boolean isWhitelisted(NameAndId nameAndId) {
        // The host is always allowed, whatever the list says.
        if (isPlayerServerOwner(nameAndId)) {
            return true;
        }

        String name = nameAndId.name();
        for (String friend : OpenToOnlineConfig.friends.get()) {
            if (friend.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the whitelist should gate logins right now. */
    public static boolean isWhitelistActive() {
        return isClientRunningOnlineServer() && OpenToOnlineConfig.whitelistMode.get();
    }
}
