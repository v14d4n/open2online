package com.v14d4n.open2online.network;

import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.network.chat.ModChat;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent.MessageTypes;
import com.v14d4n.open2online.network.nat.IUPnPLibrary;
import com.v14d4n.open2online.network.nat.UPnPLibraries;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class UPnPHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Open2Online");

    private static boolean closePortAfterLogout;
    private static IUPnPLibrary upnp;

    private UPnPHandler() {
    }

    public static boolean openPort(int port) {
        UPnPLibraries selected = UPnPLibraries.getById(OpenToOnlineConfig.libraryId.get());
        return selected.isAuto() ? openPortAutomatically(port) : openPortWith(selected, port);
    }

    /** A backend the player picked by hand: report exactly what happened, and stop at the first failure. */
    private static boolean openPortWith(UPnPLibraries library, int port) {
        IUPnPLibrary handler = library.getHandler();

        if (!handler.isUPnPAvailable()) {
            ModChat.send(ModChatTranslatableComponent.of(unavailableKey(library), MessageTypes.ERROR));
            handler.discard();
            return false;
        }
        ModChat.send(ModChatTranslatableComponent.of(availableKey(library)));
        announceOpening(port);

        if (!map(handler, port, true)) {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.error.portOpening", MessageTypes.ERROR));
            handler.discard();
            return false;
        }

        upnp = handler;
        return true;
    }

    /**
     * Walks the backends until one maps the port, starting with whichever worked last time.
     *
     * <p>Each attempt says which backend it is about to try, because discovery can take tens of
     * seconds and silence for that long reads as a hang. Failures stay quiet: the next line naming
     * the next backend already says the previous one did not work, and three spelled-out failures
     * would bury the chat in things the player cannot act on.
     */
    private static boolean openPortAutomatically(int port) {
        announceOpening(port);

        for (UPnPLibraries candidate : autoCandidates()) {
            IUPnPLibrary handler;
            try {
                handler = candidate.getHandler();
            } catch (RuntimeException e) {
                LOGGER.warn("Could not create the {} backend", candidate, e);
                continue;
            }

            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.tryingLibrary",
                    MessageTypes.OK, candidate.caption()));

            boolean opened;
            try {
                opened = handler.isUPnPAvailable() && map(handler, port, false);
            } catch (RuntimeException e) {
                // A backend blowing up must not abort the whole chain.
                LOGGER.warn("Backend {} failed while opening port {}", candidate, port, e);
                opened = false;
            }

            if (opened) {
                upnp = handler;
                rememberAutoLibrary(candidate);
                // Which backend won is already on screen, one line up, so this only has to confirm
                // the outcome — the same wording the manual path uses.
                ModChat.send(ModChatTranslatableComponent.of("chat.open2online.portIsOpen"));
                return true;
            }

            handler.discard();
        }

        // Nothing worked, so the remembered choice is stale — start clean next time.
        forgetAutoLibrary();
        ModChat.send(ModChatTranslatableComponent.of("chat.open2online.error.autoAllFailed", MessageTypes.ERROR));
        return false;
    }

    /** The backend that succeeded last time goes first; the rest keep their declared order. */
    private static List<UPnPLibraries> autoCandidates() {
        UPnPLibraries remembered = UPnPLibraries.backendById(OpenToOnlineConfig.autoLibraryId.get());
        if (remembered == null) {
            return UPnPLibraries.AUTO_ORDER;
        }

        List<UPnPLibraries> ordered = new ArrayList<>(UPnPLibraries.AUTO_ORDER.size());
        ordered.add(remembered);
        for (UPnPLibraries library : UPnPLibraries.AUTO_ORDER) {
            if (library != remembered) {
                ordered.add(library);
            }
        }
        return ordered;
    }

    private static boolean map(IUPnPLibrary handler, int port, boolean verbose) {
        if (handler.isMappedTCP(port)) {
            if (verbose) {
                ModChat.send(ModChatTranslatableComponent.of("chat.open2online.portIsAlreadyOpen"));
            }
            return true;
        }

        if (handler.openPortTCP(port)) {
            if (verbose) {
                ModChat.send(ModChatTranslatableComponent.of("chat.open2online.portIsOpen"));
            }
            return true;
        }

        return false;
    }

    private static void announceOpening(int port) {
        ModChat.send(ModChatTranslatableComponent.of("chat.open2online.openingTcpPort")
                .append(Component.literal(" " + port + "...")));
    }

    /** PortMapper is the only backend that also speaks NAT-PMP and PCP, so it gets its own wording. */
    private static String availableKey(UPnPLibraries library) {
        return library == UPnPLibraries.PORTMAPPER
                ? "chat.open2online.upnpNatPcpIsAvailable"
                : "chat.open2online.upnpIsAvailable";
    }

    private static String unavailableKey(UPnPLibraries library) {
        return library == UPnPLibraries.PORTMAPPER
                ? "chat.open2online.error.upnpNatPcpIsNotAvailable"
                : "chat.open2online.error.upnpIsNotAvailable";
    }

    private static void rememberAutoLibrary(UPnPLibraries library) {
        if (OpenToOnlineConfig.autoLibraryId.get() != library.getId()) {
            OpenToOnlineConfig.autoLibraryId.set(library.getId());
            OpenToOnlineConfig.autoLibraryId.save();
        }
    }

    private static void forgetAutoLibrary() {
        if (OpenToOnlineConfig.autoLibraryId.get() != UPnPLibraries.AUTO.getId()) {
            OpenToOnlineConfig.autoLibraryId.set(UPnPLibraries.AUTO.getId());
            OpenToOnlineConfig.autoLibraryId.save();
        }
    }

    public static boolean closePort(int port) {
        if (upnp == null) {
            throw new IllegalStateException("No UPnP backend has been selected");
        }

        ModChat.send(ModChatTranslatableComponent.of("chat.open2online.closingTcpPort")
                .append(Component.literal(" " + port + "...")));

        if (!upnp.isMappedTCP(port)) {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.portIsAlreadyClosed"));
        } else if (upnp.closePortTCP(port)) {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.portIsClosed"));
        } else {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.error.portClosing", MessageTypes.ERROR));
            return false;
        }

        return true;
    }

    /** What the router told the backend that mapped the port, if one did. */
    public static Optional<String> externalAddress() {
        return upnp == null ? Optional.empty() : upnp.externalAddress();
    }

    public static void closePortAfterLogout(boolean value) {
        closePortAfterLogout = value;
    }

    /** Fired for every player leaving; only the host quitting should tear the mapping down. */
    public static void onPlayerLoggedOut(ServerPlayer player) {
        if (!closePortAfterLogout) {
            return;
        }

        // Fires for every player on the host's integrated server, so the leaver has to be identified
        // before anything is torn down — a guest quitting must not close the host's port.
        if (!ServerHandler.isPlayerServerOwner(player.nameAndId())) {
            return;
        }

        closePortAfterLogout(false);
        ServerHandler.refreshWindowTitle();

        int port = OpenToOnlineConfig.port.get();
        if (upnp != null && upnp.isMappedTCP(port)) {
            upnp.closePortTCP(port);
        }
    }

    public static boolean isPortAvailable(int port) {
        try (Socket ignored = new Socket("localhost", port)) {
            return false;
        } catch (IOException ignored) {
            return true;
        }
    }

    public static boolean getClosePortAfterLogout() {
        return closePortAfterLogout;
    }
}
