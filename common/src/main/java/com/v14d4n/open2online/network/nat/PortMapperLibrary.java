package com.v14d4n.open2online.network.nat;

import com.offbynull.portmapper.PortMapperFactory;
import com.offbynull.portmapper.gateway.Bus;
import com.offbynull.portmapper.gateway.Gateway;
import com.offbynull.portmapper.gateways.network.NetworkGateway;
import com.offbynull.portmapper.gateways.network.internalmessages.KillNetworkRequest;
import com.offbynull.portmapper.gateways.process.ProcessGateway;
import com.offbynull.portmapper.gateways.process.internalmessages.KillProcessRequest;
import com.offbynull.portmapper.mapper.MappedPort;
import com.offbynull.portmapper.mapper.PortMapper;
import com.offbynull.portmapper.mapper.PortType;
import com.v14d4n.open2online.config.OpenToOnlineConfig;

import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * The only backend here that speaks NAT-PMP and PCP in addition to UPnP.
 */
public class PortMapperLibrary implements IUPnPLibrary {
    private static final long LIFETIME_SECONDS = 20L;
    /** Renewed at half the lifetime, so a missed round still leaves the mapping standing. */
    private static final long RENEWAL_PERIOD_SECONDS = LIFETIME_SECONDS / 2;

    private final Gateway network = NetworkGateway.create();
    private final Gateway process = ProcessGateway.create();
    private final Bus networkBus = network.getBus();
    private final Bus processBus = process.getBus();

    private final PortLease lease = new PortLease("Open2Online PortMapper lease");

    private List<PortMapper> mappers;
    private PortMapper currentMapper;
    /** Replaced by every renewal, and read from the thread that publishes and later closes. */
    private volatile MappedPort mappedPort;
    private volatile boolean isMapped;

    @Override
    public boolean isUPnPAvailable() {
        try {
            mappers = PortMapperFactory.discover(networkBus, processBus);
        } catch (Exception e) {
            return false;
        }
        return !mappers.isEmpty();
    }

    @Override
    public boolean isMappedTCP(int port) {
        return isMapped;
    }

    /**
     * Opening, closing and renewing take turns.
     *
     * <p>Cancelling the schedule cannot stop a renewal already on the bus, so without this an unmap
     * could land in the middle of one and be undone by the refresh that follows it. Holding the lock
     * makes the unmap wait out the run that started first, and then remove whatever it left.
     */
    @Override
    public synchronized boolean openPortTCP(int port) {
        // The index of the mapper that worked last time is cached, so the usual case skips discovery.
        int cachedIndex = OpenToOnlineConfig.portMapperIndex.get();
        if (cachedIndex >= 0 && cachedIndex < mappers.size() && tryMap(cachedIndex, port)) {
            return true;
        }

        for (int i = 0; i < mappers.size(); i++) {
            if (i == cachedIndex) {
                continue;
            }
            if (tryMap(i, port)) {
                OpenToOnlineConfig.portMapperIndex.set(i);
                OpenToOnlineConfig.portMapperIndex.save();
                return true;
            }
        }

        return false;
    }

    private boolean tryMap(int index, int port) {
        try {
            mappedPort = mappers.get(index).mapPort(PortType.TCP, port, port, 1);
            currentMapper = mappers.get(index);
        } catch (Exception e) {
            return false;
        }
        lease.renewEvery(RENEWAL_PERIOD_SECONDS, TimeUnit.SECONDS, this::renewLease);
        isMapped = true;
        return true;
    }

    /** Comes back with the mapping itself, so there is nothing left to ask the router afterwards. */
    @Override
    public Optional<String> externalAddress() {
        return mappedPort == null
                ? Optional.empty()
                : Optional.ofNullable(mappedPort.getExternalAddress()).map(InetAddress::getHostAddress);
    }

    @Override
    public synchronized boolean closePortTCP(int port) {
        // Called off first, so a renewal still queued behind this lock finds nothing to do.
        lease.cancel();

        // Nothing was ever mapped, so nothing failed to close. The host quitting closes the port
        // without asking whether it is open, and that has to be answerable.
        if (currentMapper == null || mappedPort == null) {
            discard();
            return true;
        }

        boolean isPortClosed = true;
        try {
            currentMapper.unmapPort(mappedPort);
        } catch (Exception e) {
            isPortClosed = false;
        } finally {
            isMapped = false;
            discard();
        }
        return isPortClosed;
    }

    /** The gateways spin up threads in the constructor, so an unused instance still has to be killed. */
    @Override
    public synchronized void discard() {
        lease.cancel();
        networkBus.send(new KillNetworkRequest());
        processBus.send(new KillProcessRequest());
    }

    private synchronized void renewLease() {
        // Asked after the lock, not before: this run may have been waiting here while an unmap went
        // through, and refreshing the mapping it just removed is exactly the mistake being avoided.
        if (lease.isCancelled()) {
            return;
        }

        try {
            mappedPort = currentMapper.refreshPort(mappedPort, LIFETIME_SECONDS * 1000L);
        } catch (InterruptedException e) {
            // Cancelling the lease interrupts a renewal in flight; there is nothing left to renew.
            Thread.currentThread().interrupt();
        }
    }
}
