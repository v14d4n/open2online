package com.v14d4n.opentoonline.network.upnp;

import com.dosse.upnp.UPnP;

public class WaifUPnPLibrary implements IUPnPLibrary {
    @Override
    public boolean isUPnPAvailable() {
        return UPnP.isUPnPAvailable();
    }

    @Override
    public boolean isMappedTCP(int port) {
        return UPnP.isMappedTCP(port);
    }

    @Override
    public boolean openPortTCP(int port) {
        return UPnP.openPortTCP(port);
    }

    @Override
    public boolean closePortTCP(int port) {
        return UPnP.closePortTCP(port);
    }
}
