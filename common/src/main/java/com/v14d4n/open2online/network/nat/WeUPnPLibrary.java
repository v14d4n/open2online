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
     * Deliberately slower than the WaifUPnP one. There the mapping expires on its own and the loop is
     * a real renewal; here it is asked for with no expiry at all, so this only catches the gateways
     * that ignore or cap that, and the ones that lose their table on a reboot.
     */
    private static final long VERIFY_INTERVAL_SECONDS = 60L;

    private static GatewayDevice gatewayDevice;

    private final PortLease lease = new PortLease("Open2Online WeUPnP lease");

    private volatile int port;

    @Override
    public boolean isUPnPAvailable() {
        return getValidGateway() != null;
    }

    @Override
    public boolean isMappedTCP(int port) {
        return getTcpPortMapping(port);
    }

    @Override
    public boolean openPortTCP(int port) {
        this.port = port;
        boolean result = addTcpPortMapping(port);

        if (result) {
            lease.renewEvery(VERIFY_INTERVAL_SECONDS, TimeUnit.SECONDS, this::verifyMapping);
        }

        return result;
    }

    @Override
    public boolean closePortTCP(int port) {
        // Called off first: a re-add landing after the delete would put the mapping straight back.
        lease.cancel();

        return deleteTcpPortMapping(port);
    }

    @Override
    public void discard() {
        lease.cancel();
    }

    /** A separate {@code GetExternalIPAddress} call; the gateway is already discovered by this point. */
    @Override
    public Optional<String> externalAddress() {
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

    private static GatewayDevice getValidGateway() {
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

    private static boolean getTcpPortMapping(int port) {
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

    private static boolean addTcpPortMapping(int port) {
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

    private static boolean deleteTcpPortMapping(int port) {
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
    private void verifyMapping() {
        if (!getTcpPortMapping(port)) {
            addTcpPortMapping(port);
            LOGGER.info("Port {} did not come back as mapped; asked the gateway for it again.", port);
        }
    }

}
