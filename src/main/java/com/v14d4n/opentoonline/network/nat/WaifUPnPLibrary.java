package com.v14d4n.opentoonline.network.nat;

import com.simtechdata.waifupnp.UPnP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WaifUPnPLibrary implements IUPnPLibrary {

    private int port;
    private final Thread updateLifetimeThread = new Thread(this::updateLifetime);

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public boolean isUPnPAvailable() {
        return WaifUPnPLibrary.log(UPnP.isUPnPAvailable());
    }

    @Override
    public boolean isMappedTCP(int port) {
        return WaifUPnPLibrary.log(UPnP.isMappedTCP(port));
    }

    @Override
    public boolean openPortTCP(int port) {
        this.port = port;
        boolean result = UPnP.openPortTCP(port);

        if (result) { updateLifetimeThread.start(); }

        return WaifUPnPLibrary.log(result);
    }

    @Override
    public boolean closePortTCP(int port) {
        boolean result = UPnP.closePortTCP(port);

        updateLifetimeThread.interrupt();

        return WaifUPnPLibrary.log(result);
    }

    private static boolean log(boolean result) {
        if (result) {
            return true;
        } else {
            LOGGER.error("[Open2Online-Log]: WaifUPnP does not support logging.");
            return false;
        }
    }

    private void updateLifetime() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (!UPnP.isMappedTCP(port)) {
                    UPnP.openPortTCP(port);
                    System.out.println("[Open2Online-Log]: Lifetime is updated.");
                } else {
                    System.out.println("[Open2Online-Log]: Port is already opened.");
                }

                // My port closes after about 10 minutes
                // But maybe it happens more often for someone else
                // The library does not allow you to set lifetime for a port

                Thread.sleep(31000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
