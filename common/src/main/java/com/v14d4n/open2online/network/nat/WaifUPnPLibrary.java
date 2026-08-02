package com.v14d4n.open2online.network.nat;

import com.simtechdata.waifupnp.UPnP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class WaifUPnPLibrary implements IUPnPLibrary {
    private static final Logger LOGGER = LoggerFactory.getLogger("Open2Online");

    /** The library exposes no way to set a lease, and mappings expire after roughly 10 minutes. */
    private static final long REFRESH_INTERVAL_SECONDS = 31L;

    private final PortLease lease = new PortLease("Open2Online WaifUPnP lease");

    private volatile int port;

    /** Not logged when false: "there is no UPnP here" is an answer, and Auto mode expects to hear it. */
    @Override
    public boolean isUPnPAvailable() {
        return UPnP.isUPnPAvailable();
    }

    /** Likewise: a port that is not mapped yet is the normal state right before opening one. */
    @Override
    public boolean isMappedTCP(int port) {
        return UPnP.isMappedTCP(port);
    }

    @Override
    public boolean openPortTCP(int port) {
        this.port = port;
        boolean result = UPnP.openPortTCP(port);

        if (result) {
            lease.renewEvery(REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS, this::renewLease);
        }

        return logFailure(result, "open");
    }

    @Override
    public boolean closePortTCP(int port) {
        // Called off first: a renewal landing after the mapping is gone would put it straight back.
        lease.cancel();

        return logFailure(UPnP.closePortTCP(port), "close");
    }

    @Override
    public Optional<String> externalAddress() {
        return Optional.ofNullable(UPnP.getExternalIP()).filter(address -> !address.isBlank());
    }

    @Override
    public void discard() {
        lease.cancel();
    }

    /** Only for the two calls that were asked to change something and did not. */
    private static boolean logFailure(boolean result, String action) {
        if (!result) {
            LOGGER.error("WaifUPnP failed to {} the port; the library does not expose a reason.", action);
        }
        return result;
    }

    private void renewLease() {
        if (!UPnP.isMappedTCP(port)) {
            UPnP.openPortTCP(port);
            LOGGER.info("Renewed the UPnP lease for port {}.", port);
        }
    }
}
