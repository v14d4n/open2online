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
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Environment(EnvType.CLIENT)
public final class UPnPHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Open2Online");

    /**
     * Whether this client is hosting a world on the internet right now.
     *
     * <p>Three threads want to know, and none of them is the one that sets it. The publish worker
     * writes it; the server thread reads it at every login, through the whitelist gate in
     * {@code MixinPlayerList}; the render thread reads it when the window title is rebuilt; and the
     * server thread takes it back when the server stops. As a plain field none of those readers was
     * promised the write at all — a player joining moments after the world went online could be let
     * in against a stale {@code false}, past the whitelist.
     *
     * <p>Atomic rather than merely {@code volatile} because stopping the server is a claim, not a
     * read: whoever flips it back to false is the one that closes the mapping, and that has to
     * happen exactly once.
     */
    private static final AtomicBoolean closePortAfterLogout = new AtomicBoolean();

    /**
     * How long a publish will wait for a previous mapping to come off the router before giving up
     * and reporting an ordinary failure. Generous for what it covers — a single call to a gateway
     * already found — and finite so that a permit lost to some future mistake costs one clear error
     * rather than every publish from then on hanging.
     */
    private static final long MAPPING_HANDOVER_TIMEOUT_MS = 10_000L;

    /**
     * The one mapping the mod may hold on the router, and the only thing a second publish ever
     * queues behind.
     *
     * <p>Held from the instant a mapping is made until it is taken off again — never around the
     * search that precedes it. That is the whole point: a publish started right after a cancel finds
     * this free, because the attempt it replaced had not mapped anything yet, and goes about its own
     * discovery in parallel with the doomed one winding down. Only if the old attempt did get as far
     * as the router does the new one wait, and then only for the unmapping.
     */
    private static final Semaphore mappingSlot = new Semaphore(1);

    /**
     * The backend holding the mapping, read by the server thread when it stops.
     *
     * <p>Not null exactly when {@link #mappingSlot} is held, which is what makes handing the slot
     * back and letting go of the backend a single decision — see {@link #takeHeldBackend()}.
     */
    private static volatile IUPnPLibrary upnp;

    private UPnPHandler() {
    }

    public static boolean openPort(int port, PublishAttempt attempt) {
        UPnPLibraries selected = UPnPLibraries.getById(OpenToOnlineConfig.libraryId.get());
        return selected.isAuto()
                ? openPortAutomatically(port, attempt)
                : openPortWith(selected, port, attempt);
    }

    /** A backend the player picked by hand: report exactly what happened, and stop at the first failure. */
    private static boolean openPortWith(UPnPLibraries library, int port, PublishAttempt attempt) {
        IUPnPLibrary handler = library.getHandler();

        if (!handler.isUPnPAvailable()) {
            attempt.say(unavailableKey(library), MessageTypes.ERROR);
            handler.discard();
            return false;
        }
        attempt.say(availableKey(library));

        // One backend means one place to stop, and this is it: the discovery behind the line above is
        // done, the router has not been touched. Checked before the announcement so that chat does not
        // promise an opening that is not going to happen.
        if (attempt.isCancelled()) {
            handler.discard();
            return false;
        }
        announceOpening(attempt, port);

        if (!mapHoldingSlot(handler, port, attempt, true)) {
            attempt.say("chat.open2online.error.portOpening", MessageTypes.ERROR);
            handler.discard();
            return false;
        }

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
    private static boolean openPortAutomatically(int port, PublishAttempt attempt) {
        announceOpening(attempt, port);

        for (UPnPLibraries candidate : autoCandidates()) {
            // Before anything is built: the PortMapper backend starts threads in its constructor.
            if (attempt.isCancelled()) {
                return false;
            }

            IUPnPLibrary handler;
            try {
                handler = candidate.getHandler();
            } catch (RuntimeException e) {
                LOGGER.warn("Could not create the {} backend", candidate, e);
                continue;
            }

            attempt.say("chat.open2online.tryingLibrary", MessageTypes.OK, candidate.caption());

            boolean opened;
            try {
                // Asked again between the two halves. Availability is the slow one — that is the
                // discovery — while the mapping after it is a single call to a gateway already found.
                // Stopping in the gap is the cheapest kind there is: nothing was mapped, so nothing
                // has to be taken back.
                opened = handler.isUPnPAvailable()
                        && !attempt.isCancelled()
                        && mapHoldingSlot(handler, port, attempt, false);
            } catch (RuntimeException e) {
                // A backend blowing up must not abort the whole chain.
                LOGGER.warn("Backend {} failed while opening port {}", candidate, port, e);
                opened = false;
            }

            if (opened) {
                rememberAutoLibrary(candidate);
                // Which backend won is already on screen, one line up, so this only has to confirm
                // the outcome — the same wording the manual path uses.
                attempt.say("chat.open2online.portIsOpen");
                return true;
            }

            handler.discard();
        }

        // A cancel is not a verdict on the backends: the remembered one keeps its place, and the
        // player hears about their own decision rather than a failure they did not cause.
        if (attempt.isCancelled()) {
            return false;
        }

        // Nothing worked, so the remembered choice is stale — start clean next time.
        forgetAutoLibrary();
        attempt.say("chat.open2online.error.autoAllFailed", MessageTypes.ERROR);
        return false;
    }

    /**
     * Maps the port while holding {@link #mappingSlot}, keeping the slot only if a mapping was made.
     *
     * <p>Waiting happens here and only here. Whoever holds the slot has a live mapping on the router,
     * so the wait is the length of an unmapping — not of somebody else's discovery.
     */
    private static boolean mapHoldingSlot(IUPnPLibrary handler, int port, PublishAttempt attempt, boolean verbose) {
        if (!acquireMappingSlot()) {
            LOGGER.warn("Gave up waiting for the previous mapping of port {} to be released", port);
            return false;
        }

        boolean mapped = false;
        try {
            mapped = map(handler, port, attempt, verbose);
            if (mapped) {
                // Written here and nowhere else, which is what keeps "holds the slot" and "owns the
                // backend" the same fact. A cancelled attempt never reaches this line, so it cannot
                // put its own backend over the one belonging to the publish that replaced it.
                upnp = handler;
            }
            return mapped;
        } finally {
            if (!mapped) {
                mappingSlot.release();
            }
        }
    }

    private static boolean acquireMappingSlot() {
        try {
            return mappingSlot.tryAcquire(MAPPING_HANDOVER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Hands the held backend over exactly once, so that two closes racing cannot both act on it and
     * give the slot back twice.
     */
    private static synchronized IUPnPLibrary takeHeldBackend() {
        IUPnPLibrary backend = upnp;
        upnp = null;
        return backend;
    }

    /** The backend that succeeded last time goes first; the rest keep their declared order. */
    private static List<UPnPLibraries> autoCandidates() {
        return UPnPLibraries.backendById(OpenToOnlineConfig.autoLibraryId.get())
                .map(UPnPHandler::startingWith)
                .orElse(UPnPLibraries.AUTO_ORDER);
    }

    private static List<UPnPLibraries> startingWith(UPnPLibraries remembered) {
        List<UPnPLibraries> ordered = new ArrayList<>(UPnPLibraries.AUTO_ORDER.size());
        ordered.add(remembered);
        for (UPnPLibraries library : UPnPLibraries.AUTO_ORDER) {
            if (library != remembered) {
                ordered.add(library);
            }
        }
        return ordered;
    }

    private static boolean map(IUPnPLibrary handler, int port, PublishAttempt attempt, boolean verbose) {
        if (handler.isMappedTCP(port)) {
            if (verbose) {
                attempt.say("chat.open2online.portIsAlreadyOpen");
            }
            return true;
        }

        if (handler.openPortTCP(port)) {
            if (verbose) {
                attempt.say("chat.open2online.portIsOpen");
            }
            return true;
        }

        return false;
    }

    private static void announceOpening(PublishAttempt attempt, int port) {
        attempt.say(ModChatTranslatableComponent.of("chat.open2online.openingTcpPort")
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

    /**
     * Takes the mapping down, and answers whether the port ended up closed.
     *
     * <p>Nothing ever mapped is not a failure to close, so it is a quiet success rather than the
     * {@code IllegalStateException} this used to throw. That exception made every caller responsible
     * for knowing whether a backend had been chosen, and the one that forgot took the publish worker
     * down with it on a LAN game. {@code PortMapperLibrary.closePortTCP} already answers the same way
     * for the same reason.
     */
    public static boolean closePort(int port) {
        IUPnPLibrary backend = takeHeldBackend();
        if (backend == null) {
            return true;
        }

        try {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.closingTcpPort")
                    .append(Component.literal(" " + port + "...")));

            if (!backend.isMappedTCP(port)) {
                ModChat.send(ModChatTranslatableComponent.of("chat.open2online.portIsAlreadyClosed"));
            } else if (backend.closePortTCP(port)) {
                ModChat.send(ModChatTranslatableComponent.of("chat.open2online.portIsClosed"));
            } else {
                ModChat.send(ModChatTranslatableComponent.of("chat.open2online.error.portClosing",
                        MessageTypes.ERROR));
                return false;
            }

            return true;
        } finally {
            mappingSlot.release();
        }
    }

    /**
     * Undoes a mapping without a word about it, for the two occasions when narrating it would be
     * noise: a publish the player called off, and a world that is no longer there to publish to.
     *
     * <p>Unconditional, deliberately. Asking whether the port is still mapped is no help, because a
     * query that fails looks exactly like an answer of "no" — and acting on that would leave the
     * mapping on the router for good. Deleting one that is not there costs nothing.
     */
    static void releaseMapping(int port) {
        IUPnPLibrary backend = takeHeldBackend();
        if (backend == null) {
            return;
        }

        try {
            backend.closePortTCP(port);
        } finally {
            mappingSlot.release();
        }
    }

    /** What the router told the backend that mapped the port, if one did. */
    public static Optional<String> externalAddress() {
        IUPnPLibrary backend = upnp;
        return backend == null ? Optional.empty() : backend.externalAddress();
    }

    public static void closePortAfterLogout(boolean value) {
        closePortAfterLogout.set(value);
    }

    /**
     * Ends the mapping together with the server that needed it.
     *
     * <p>This is the whole lifetime in one hook: the integrated server stops both when the world is
     * left and when the game is closed with it still open. A player event cannot say the same — in
     * the second case nobody ever logs out, which is how a mapping used to outlive the game.
     *
     * <p>It also removes a question that had no good answer. Watching for the host's logout meant
     * telling the host apart from a guest by name, and with licence checking off a name is not
     * something anyone can verify. Nobody has to be identified for a server to stop.
     */
    public static void onServerStopping(MinecraftServer server) {
        // Claiming the flag and acting on it is one step: whoever takes it away from true is the one
        // that closes the mapping. The synchronized this replaces only kept two stops apart from each
        // other, and left the publish worker — the thread that actually writes here — outside.
        if (!closePortAfterLogout.compareAndSet(true, false)) {
            return;
        }

        releaseMapping(OpenToOnlineConfig.port.get());
    }

    public static boolean getClosePortAfterLogout() {
        return closePortAfterLogout.get();
    }
}
