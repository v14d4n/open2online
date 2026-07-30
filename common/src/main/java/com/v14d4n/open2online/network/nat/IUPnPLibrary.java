package com.v14d4n.open2online.network.nat;

public interface IUPnPLibrary {
    boolean isUPnPAvailable();

    boolean isMappedTCP(int port);

    boolean openPortTCP(int port);

    boolean closePortTCP(int port);

    /**
     * Releases whatever the backend started up front when it is abandoned without ever mapping a
     * port. Auto mode instantiates candidates until one works, so the ones it walks past have to be
     * torn down explicitly — otherwise their background threads outlive the attempt.
     */
    default void discard() {
    }
}
