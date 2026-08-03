package com.v14d4n.open2online.network.nat;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class WeUPnPLibrary implements IUPnPLibrary {
    private static final Logger LOGGER = LoggerFactory.getLogger("Open2Online");

    /**
     * This backend and WaifUPnP put the same {@code NewLeaseDuration} 0 on the wire, so neither
     * schedule renews a lease: both exist for gateways that ignore or cap the request, and for tables
     * lost to a reboot.
     *
     * <p>The same job, so the same interval as {@code WaifUPnPLibrary}. The number is the one from
     * 1.16.5, measured against a router that dropped mappings after roughly ten minutes; it is the
     * only figure here anyone ever checked against real hardware.
     */
    private static final long VERIFY_INTERVAL_SECONDS = 31L;

    /**
     * Found once per instance, which is to say once per attempt at publishing.
     *
     * <p>It used to be static, and that bought a saved discovery — three seconds, the library's own
     * default timeout — at the price of one router being remembered for the life of the game. Two
     * attempts running at once, which cancelling made an ordinary thing, would race to fill it; a
     * player whose network changed under them kept talking to a gateway that was no longer there; and
     * {@link #discard()} could not undo any of it, because there was nothing instance-shaped to undo.
     * Paying the three seconds again per attempt buys all of that back.
     */
    private GatewayDevice gatewayDevice;

    private final PortLease lease = new PortLease("Open2Online WeUPnP lease");

    private volatile int port;

    @Override
    public synchronized boolean isUPnPAvailable() {
        return getValidGateway() != null;
    }

    @Override
    public synchronized boolean isMappedTCP(int port) {
        return getTcpPortMapping(port);
    }

    /**
     * Opening, closing and verifying take turns.
     *
     * <p>Cancelling the schedule cannot stop a verification already inside a call to the router, so
     * without this a close could land in the middle of one and be undone by its second half. Holding
     * the lock makes the close wait out the run that started first, and then delete whatever it left.
     */
    @Override
    public synchronized boolean openPortTCP(int port) {
        this.port = port;
        boolean result = addTcpPortMapping(port);

        if (result) {
            lease.renewEvery(VERIFY_INTERVAL_SECONDS, TimeUnit.SECONDS, this::verifyMapping);
        }

        return result;
    }

    @Override
    public synchronized boolean closePortTCP(int port) {
        // Called off first, so a verification still queued behind this lock finds nothing to do.
        lease.cancel();

        return deleteTcpPortMapping(port);
    }

    @Override
    public synchronized void discard() {
        lease.cancel();
    }

    /** A separate {@code GetExternalIPAddress} call; the gateway is already discovered by this point. */
    @Override
    public synchronized Optional<String> externalAddress() {
        GatewayDevice gateway = getValidGateway();
        if (gateway == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(gateway.getExternalIPAddress());
        } catch (IOException | SAXException e) {
            LOGGER.error("Failed to read the external address from the gateway", e);
            return Optional.empty();
        }
    }

    private GatewayDevice getValidGateway() {
        if (gatewayDevice == null) {
            try {
                GatewayDiscover discover = new GatewayDiscover();
                discover.discover();
                gatewayDevice = discover.getValidGateway();
            } catch (IOException | ParserConfigurationException | SAXException e) {
                LOGGER.error("Gateway discovery failed", e);
            }
        }
        return gatewayDevice;
    }

    private boolean getTcpPortMapping(int port) {
        GatewayDevice gateway = getValidGateway();
        if (gateway == null) {
            return false;
        }
        try {
            // A fresh entry per call rather than one kept around: it is only an out-parameter, and
            // the verification below asks from a thread of its own while the game may be closing the
            // port from another.
            return gateway.getSpecificPortMappingEntry(port, "TCP", new PortMappingEntry());
        } catch (IOException | SAXException e) {
            LOGGER.error("Failed to query TCP mapping for port {}", port, e);
        }
        return false;
    }

    private boolean addTcpPortMapping(int port) {
        GatewayDevice gateway = getValidGateway();
        if (gateway == null) {
            return false;
        }
        InetAddress localAddress = gateway.getLocalAddress();
        try {
            // The library sends NewLeaseDuration 0 — no expiry — which is why the schedule above only
            // checks the mapping rather than renewing it.
            return gateway.addPortMapping(port, port, localAddress.getHostAddress(), "TCP", "Minecraft");
        } catch (IOException | SAXException e) {
            LOGGER.error("Failed to add TCP mapping for port {}", port, e);
        }
        return false;
    }

    private boolean deleteTcpPortMapping(int port) {
        GatewayDevice gateway = getValidGateway();
        if (gateway == null) {
            return false;
        }
        try {
            return gateway.deletePortMapping(port, "TCP");
        } catch (IOException | SAXException e) {
            LOGGER.error("Failed to delete TCP mapping for port {}", port, e);
        }
        return false;
    }

    /**
     * Asks the gateway whether the mapping is still there, and re-adds it if the answer is not a
     * clear yes.
     *
     * <p>The query cannot tell "the mapping is gone" from "the gateway did not answer", so the log
     * line does not pretend to know which of the two happened. Re-adding is right either way: a
     * mapping that is already there is simply overwritten with itself.
     */
    private synchronized void verifyMapping() {
        // Asked after the lock, not before: this run may have been waiting here while a close went
        // through, and re-adding the mapping it just deleted is exactly the mistake being avoided.
        if (lease.isCancelled()) {
            return;
        }

        if (!getTcpPortMapping(port)) {
            addTcpPortMapping(port);
            LOGGER.info("Port {} did not come back as mapped; asked the gateway for it again.", port);
        }
    }

}
