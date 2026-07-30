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

import java.util.List;

/**
 * The only backend here that speaks NAT-PMP and PCP in addition to UPnP.
 */
public class PortMapperLibrary implements IUPnPLibrary {
    private static final long LIFETIME_SECONDS = 20L;

    private final Gateway network = NetworkGateway.create();
    private final Gateway process = ProcessGateway.create();
    private final Bus networkBus = network.getBus();
    private final Bus processBus = process.getBus();

    private final Thread updateLifetimeThread = new Thread(this::updateLifetime, "Open2Online PortMapper lease");

    private List<PortMapper> mappers;
    private PortMapper currentMapper;
    private MappedPort mappedPort;
    private boolean isMapped;

    public PortMapperLibrary() {
        updateLifetimeThread.setDaemon(true);
    }

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

    @Override
    public boolean openPortTCP(int port) {
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
        updateLifetimeThread.start();
        isMapped = true;
        return true;
    }

    @Override
    public boolean closePortTCP(int port) {
        boolean isPortClosed = true;
        updateLifetimeThread.interrupt();
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
    public void discard() {
        networkBus.send(new KillNetworkRequest());
        processBus.send(new KillProcessRequest());
    }

    private void updateLifetime() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                mappedPort = currentMapper.refreshPort(mappedPort, LIFETIME_SECONDS * 1000L);
                Thread.sleep(LIFETIME_SECONDS * 1000L / 2);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
