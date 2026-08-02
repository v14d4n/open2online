package com.v14d4n.open2online.network.nat;

import java.util.Optional;

public interface IUPnPLibrary {
    boolean isUPnPAvailable();

    boolean isMappedTCP(int port);

    boolean openPortTCP(int port);

    boolean closePortTCP(int port);

    /**
     * The address the router named for its own side of the mapping, if the backend asked for it.
     *
     * <p>Every protocol here carries one: UPnP-IGD answers {@code GetExternalIPAddress}, NAT-PMP has
     * an opcode for it, and a PCP mapping response states the address it was assigned. Whether that
     * address is any use to someone outside is a separate question — see the caller.
     */
    default Optional<String> externalAddress() {
        return Optional.empty();
    }

    /**
     * Releases whatever the backend started up front when it is abandoned without ever mapping a
     * port. Auto mode instantiates candidates until one works, so the ones it walks past have to be
     * torn down explicitly — otherwise their background threads outlive the attempt.
     */
    default void discard() {
    }
}
