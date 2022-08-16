package com.v14d4n.opentoonline.network.nat;

public interface IUPnPLibrary {
    boolean isUPnPAvailable();
    boolean isMappedTCP(int port);
    boolean openPortTCP(int port);
    boolean closePortTCP(int port);
}
