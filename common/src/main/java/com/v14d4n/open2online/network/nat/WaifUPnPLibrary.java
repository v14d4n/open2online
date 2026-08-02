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

    @Override
    public boolean isUPnPAvailable() {
        return log(UPnP.isUPnPAvailable());
    }

    @Override
    public boolean isMappedTCP(int port) {
        return log(UPnP.isMappedTCP(port));
    }

    @Override
    public boolean openPortTCP(int port) {
        this.port = port;
        boolean result = UPnP.openPortTCP(port);

        if (result) {
            updateLifetimeThread.start();
        }

        return log(result);
    }

    @Override
    public boolean closePortTCP(int port) {
        boolean result = UPnP.closePortTCP(port);

        updateLifetimeThread.interrupt();

        return log(result);
    }

    @Override
    public Optional<String> externalAddress() {
        return Optional.ofNullable(UPnP.getExternalIP()).filter(address -> !address.isBlank());
    }

    @Override
    public void discard() {
        updateLifetimeThread.interrupt();
    }

    private static boolean log(boolean result) {
        if (!result) {
            LOGGER.error("WaifUPnP reported a failure; the library does not expose a reason.");
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
