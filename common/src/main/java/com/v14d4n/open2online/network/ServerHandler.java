package com.v14d4n.open2online.network;

import com.google.common.net.InetAddresses;
import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.mixin.MinecraftTitleInvoker;
import com.v14d4n.open2online.network.chat.ModChat;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public final class ServerHandler {
    /** Two independent echo services, in case one is blocked or down. */
    private static final List<String> EXTERNAL_IP_SERVICES = List.of(
            "https://checkip.amazonaws.com",
            "https://api.ipify.org");
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(3);

    /** Placeholder the config ships with, meaning "no address has ever been resolved". */
    private static final String DEFAULT_IP = "0.0.0.0";

    private ServerHandler() {
    }

    /**
     * @param attempt everything above the call to {@code publishServer} is still callable off, and
     *                everything below it is not — see {@code PublishAttempt}
     */
    public static void startServer(int port, int maxPlayers, GameType gameMode, boolean allowCommands,
                                   boolean online, PublishAttempt attempt) {
        if (online && !UPnPHandler.openPort(port, attempt)) {
            return;
        }

        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();

        // The world can be left while the port is still being mapped; the mapping must not outlive it.
        // Nobody is left to read chat by then, so this goes quietly.
        if (server == null) {
            UPnPHandler.releaseMapping(port);
            return;
        }

        // The point of no return, taken as one step rather than as a question and then an answer:
        // between checking and publishing there is room for a cancel to land, and honouring it after
        // the world is already online is not something that can be done.
        //
        // Losing the race means the player called it off first — so the mapping goes back and nothing
        // is said, because cancel() has already said the only thing worth saying.
        if (!attempt.commit()) {
            UPnPHandler.releaseMapping(port);
            return;
        }

        // Must land before publishing: usesAuthentication is read when a client connects, to fill in
        // the shouldAuthenticate flag of the login handshake.
        applyLicenceRequirement(server);

        if (!server.publishServer(gameMode, allowCommands, port)) {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.error.publishFailed",
                    ModChatTranslatableComponent.MessageTypes.ERROR));
            // A genuine failure, so this one is narrated: the player is watching a publish they asked
            // for come apart, and the port going back is part of that story.
            UPnPHandler.closePort(port);
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

        // Literally the class Minecraft parses this back with when someone pastes it into the server
        // list, so whatever it writes is by definition readable there. It is a wrapper over Guava's
        // HostAndPort, which is where the brackets around an IPv6 literal come from —
        // "[2001:db8::1]:25565", where plain concatenation produced something unparseable.
        String address = new ServerAddress(resolveExternalIP(), port).toString();
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
        // Asked once — for some backends this is a round trip to the router — and then judged twice:
        // whether it can be shown, and failing that, why not.
        Optional<String> routerSaid = UPnPHandler.externalAddress();
        Optional<String> fromRouter = routerSaid.filter(ServerHandler::isReachableFromOutside);
        Optional<String> fetched = fromRouter.or(ServerHandler::fetchExternalIP);
        String lastIP = OpenToOnlineConfig.lastIP.get();

        if (fetched.isPresent()) {
            // Only worth saying when there is an address to qualify. The paths below already warn,
            // and more loudly, about one that is stale or missing altogether.
            if (fromRouter.isEmpty()) {
                // Which story to tell depends on what the router actually said. Anything unusable —
                // including the 0.0.0.0 a router reports when it believes its WAN is down, which it
                // plainly is not, since everything else on this path just worked — falls back to the
                // vaguest of the three.
                String warning = routerSaid.flatMap(ServerHandler::parseAddress)
                        .filter(reported -> !reported.isAnyLocalAddress())
                        .map(ServerHandler::describeUnusableRouterAddress)
                        .orElse("chat.open2online.warn.routerIPUnknown");
                ModChat.send(ModChatTranslatableComponent.of(warning,
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
     * Which of the two NAT stories the router's own address tells.
     *
     * <p>Worth spelling out, because both look exactly like success and are not. The address printed
     * afterwards is genuinely the one the world sees, the port genuinely is open on the router, and
     * players still cannot connect — because something else sits between that router and the
     * internet. From inside the game there is no other way for anyone to work that out.
     */
    private static String describeUnusableRouterAddress(InetAddress reported) {
        return isCarrierGradeNat(reported)
                ? "chat.open2online.warn.carrierNat"
                : "chat.open2online.warn.behindRouter";
    }

    private static boolean isReachableFromOutside(String address) {
        return parseAddress(address).filter(ServerHandler::isPublic).isPresent();
    }

    /**
     * Parses a literal of either family without ever consulting DNS — which is the whole reason this
     * does not use {@code InetAddress.getByName}, since that treats anything it cannot parse as a
     * hostname and goes asking a name server about it.
     */
    private static Optional<InetAddress> parseAddress(String address) {
        return InetAddresses.isInetAddress(address)
                ? Optional.of(InetAddresses.forString(address))
                : Optional.empty();
    }

    private static boolean isPublic(InetAddress address) {
        return !address.isAnyLocalAddress() && !address.isLoopbackAddress()
                && !address.isMulticastAddress()
                && !isSiteInternal(address) && !isCarrierGradeNat(address);
    }

    /**
     * 100.64.0.0/10 — the range an ISP keeps for its own layer of NAT, and the one case where the
     * router is telling the truth and the server is still unreachable. Java has no test for it.
     *
     * <p>IPv4 only, deliberately: IPv6 has no equivalent, because a carrier running one has no reason
     * to hand out addresses it will then hide.
     */
    private static boolean isCarrierGradeNat(InetAddress address) {
        if (!(address instanceof Inet4Address)) {
            return false;
        }

        byte[] octets = address.getAddress();
        int second = octets[1] & 0xFF;
        return (octets[0] & 0xFF) == 100 && second >= 64 && second <= 127;
    }

    /** Addresses that belong to somebody's own network: another router in front of this one. */
    private static boolean isSiteInternal(InetAddress address) {
        if (address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
            return true;
        }

        // fc00::/7, the unique local addresses — IPv6's answer to the private ranges, and not covered
        // above: isSiteLocalAddress only knows the deprecated fec0::/10. The mask takes fc00 and fd00.
        return address instanceof Inet6Address && (address.getAddress()[0] & 0xFE) == 0xFC;
    }

    /**
     * The address as the internet sees it, asked of both services at once.
     *
     * <p>They used to be tried one after the other, twice round, with a pause in between — which
     * meant a service that had gone quiet cost the full timeout before the other was even asked, and
     * the pause was there to stop the second round treading on the first. Asking together removes
     * the reason for both: one service being down now costs nothing, because the answer to the same
     * question was already on its way from the other.
     */
    private static Optional<String> fetchExternalIP() {
        List<CompletableFuture<Optional<String>>> lookups = EXTERNAL_IP_SERVICES.stream()
                .map(service -> Http.getAsync(service, HTTP_TIMEOUT)
                        .thenApply(body -> body.flatMap(ServerHandler::readReportedAddress)))
                .toList();

        // Waited on in the order they are listed, so the first service still wins when both answer.
        // findFirst stops there and never waits on the rest.
        return lookups.stream()
                .map(CompletableFuture::join)
                .flatMap(Optional::stream)
                .findFirst();
    }

    /** The address an echo service reported, empty if what came back was not one. */
    private static Optional<String> readReportedAddress(String body) {
        // Only the shape is checked, deliberately less than the router's answer is put through. An
        // echo service reports the address its connection arrived from, which seen from the internet
        // is public by definition — there is no non-public answer here to reject. The router, by
        // contrast, reports its own WAN side, and that really can be a carrier or a private address.
        //
        // Both families pass. On a dual-stack connection the request leaves over IPv4 anyway, so an
        // IPv6 reply means IPv4 was not available at all — precisely the case where refusing it would
        // leave the player staring at the 0.0.0.0 placeholder instead of a usable address.
        return body.lines()
                .findFirst()
                .map(String::trim)
                .filter(candidate -> parseAddress(candidate).isPresent());
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
