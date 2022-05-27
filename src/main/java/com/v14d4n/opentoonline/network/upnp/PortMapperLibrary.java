package com.v14d4n.opentoonline.network.upnp;

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

import java.util.List;

public class PortMapperLibrary implements IUPnPLibrary {

    private final Gateway network = NetworkGateway.create();
    private final Gateway process = ProcessGateway.create();
    private final Bus networkBus = network.getBus();
    private final Bus processBus = process.getBus();

    private List<PortMapper> mappers;
    private PortMapper currentMapper;
    private MappedPort mappedPort;

    private final Thread updateLifetimeThread = new Thread(this::updateLifetime);

    private final long lifetime = 20L;
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
        return mappers.size() > 0;
    }

    @Override
    public boolean isMappedTCP(int port) {
        return isMapped;
    }

    @Override
    public boolean openPortTCP(int port) {
        for (PortMapper mapper : mappers) {
            try {
                mappedPort = mapper.mapPort(PortType.TCP, port, port, 1);
                currentMapper = mapper;
            } catch (Exception e) {
                continue;
            }
            updateLifetimeThread.start();
            isMapped = true;
            return true;
        }
        return false;
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
            networkBus.send(new KillNetworkRequest());
            processBus.send(new KillProcessRequest());
        }
        return isPortClosed;
    }

    private void updateLifetime() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                mappedPort = currentMapper.refreshPort(mappedPort, lifetime * 1000L);
                Thread.sleep(lifetime * 1000L / 2);
            }
        } catch (InterruptedException ignored) {

        }
    }
}
