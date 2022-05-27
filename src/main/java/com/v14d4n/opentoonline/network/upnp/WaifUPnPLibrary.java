package com.v14d4n.opentoonline.network.upnp;

import com.simtechdata.waifupnp.UPnP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WaifUPnPLibrary implements IUPnPLibrary {

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
        return WaifUPnPLibrary.log(UPnP.openPortTCP(port));
    }

    @Override
    public boolean closePortTCP(int port) {
        return WaifUPnPLibrary.log(UPnP.closePortTCP(port));
    }

    private static boolean log(boolean result) {
        if (result) {
            return true;
        } else {
            LOGGER.error("[Open2Online-Log]: WaifUPnP does not support logging.");
            return false;
        }
    }
}
