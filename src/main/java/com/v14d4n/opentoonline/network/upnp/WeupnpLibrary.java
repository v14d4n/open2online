package com.v14d4n.opentoonline.network.upnp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.InetAddress;

public class WeupnpLibrary implements IUPnPLibrary {

    private static final Logger LOGGER = LogManager.getLogger();
    private static GatewayDevice gatewayDevice;
    private static PortMappingEntry portMapping;

    @Override
    public boolean isUPnPAvailable() {
        return WeupnpLibrary.getValidGateway() != null;
    }

    @Override
    public boolean isMappedTCP(int port) {
        return WeupnpLibrary.getTcpPortMapping(port);
    }

    @Override
    public boolean openPortTCP(int port) {
        return WeupnpLibrary.addTcpPortMapping(port);
    }

    @Override
    public boolean closePortTCP(int port) {
        return WeupnpLibrary.deleteTcpPortMapping(port);
    }

    private static GatewayDevice getValidGateway() {
        if (gatewayDevice == null) {
            try {
                GatewayDiscover discover = new GatewayDiscover();
                discover.discover();
                gatewayDevice = discover.getValidGateway();
            } catch (IOException | ParserConfigurationException | SAXException e) {
                LOGGER.error("[Open2Online-Log]: "  + e.getMessage());
            }
        }
        return gatewayDevice;
    }

    private static PortMappingEntry getPortMappingEntry() {
        if (portMapping == null) {
            portMapping = new PortMappingEntry();
        }
        return portMapping;
    }

    private static boolean getTcpPortMapping(int port) {
        try {
            return WeupnpLibrary.getValidGateway().getSpecificPortMappingEntry(port, "TCP", WeupnpLibrary.getPortMappingEntry());
        } catch (IOException | SAXException e) {
            LOGGER.error("[Open2Online-Log]: "  + e.getMessage());
        }
        return false;
    }

    private static boolean addTcpPortMapping(int port) {
        InetAddress localAddress = WeupnpLibrary.getValidGateway().getLocalAddress();
        try {
            return WeupnpLibrary.getValidGateway().addPortMapping(port, port, localAddress.getHostAddress(), "TCP", "Minecraft");
        } catch (IOException | SAXException e) {
            LOGGER.error("[Open2Online-Log]: "  + e.getMessage());
        }
        return false;
    }

    private static boolean deleteTcpPortMapping(int port) {
        try {
            return WeupnpLibrary.getValidGateway().deletePortMapping(port, "TCP");
        } catch (IOException | SAXException e) {
            LOGGER.error("[Open2Online-Log]: "  + e.getMessage());
        }
        return false;
    }
}
