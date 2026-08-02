package com.v14d4n.open2online.network.nat;

import com.simtechdata.waifupnp.UPnP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class WaifUPnPLibrary implements IUPnPLibrary {
    private static final Logger LOGGER = LoggerFactory.getLogger("Open2Online");

    /** The library exposes no way to set a lease, and mappings expire after roughly 10 minutes. */
    private static final long REFRESH_INTERVAL_MS = 31_000L;

    private final Thread updateLifetimeThread = new Thread(this::updateLifetime, "Open2Online WaifUPnP lease");

    private int port;

    public WaifUPnPLibrary() {
        updateLifetimeThread.setDaemon(true);
    }

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
            updateLifetimeThread.start();
        }

        return logFailure(result, "open");
    }

    @Override
    public boolean closePortTCP(int port) {
        boolean result = UPnP.closePortTCP(port);

        updateLifetimeThread.interrupt();

        return logFailure(result, "close");
    }

    @Override
    public Optional<String> externalAddress() {
        return Optional.ofNullable(UPnP.getExternalIP()).filter(address -> !address.isBlank());
    }

    @Override
    public void discard() {
        updateLifetimeThread.interrupt();
    }

    /** Only for the two calls that were asked to change something and did not. */
    private static boolean logFailure(boolean result, String action) {
        if (!result) {
            LOGGER.error("WaifUPnP failed to {} the port; the library does not expose a reason.", action);
        }
        return result;
    }

    private void updateLifetime() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (!UPnP.isMappedTCP(port)) {
                    UPnP.openPortTCP(port);
                    LOGGER.info("Renewed the UPnP lease for port {}.", port);
                }

                Thread.sleep(REFRESH_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
