package com.v14d4n.open2online.network.nat;

import com.v14d4n.open2online.network.chat.ModChat;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

public class WeUPnPLibrary implements IUPnPLibrary {
    private static final Logger LOGGER = LoggerFactory.getLogger("Open2Online");

    private static GatewayDevice gatewayDevice;
    private static PortMappingEntry portMapping;

    @Override
    public boolean isUPnPAvailable() {
        return getValidGateway() != null;
    }

    @Override
    public boolean isMappedTCP(int port) {
        return getTcpPortMapping(port);
    }

    @Override
    public boolean openPortTCP(int port) {
        return addTcpPortMapping(port);
    }

    @Override
    public boolean closePortTCP(int port) {
        return deleteTcpPortMapping(port);
    }

    private static GatewayDevice getValidGateway() {
        if (gatewayDevice == null) {
            try {
                GatewayDiscover discover = new GatewayDiscover();
                discover.discover();
                gatewayDevice = discover.getValidGateway();
            } catch (IOException | ParserConfigurationException | SAXException e) {
                LOGGER.error("Gateway discovery failed", e);
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
        GatewayDevice gateway = getValidGateway();
        if (gateway == null) {
            return false;
        }
        try {
            return gateway.getSpecificPortMappingEntry(port, "TCP", getPortMappingEntry());
        } catch (IOException | SAXException e) {
            LOGGER.error("Failed to query TCP mapping for port {}", port, e);
        }
        return false;
    }

    private static boolean addTcpPortMapping(int port) {
        GatewayDevice gateway = getValidGateway();
        if (gateway == null) {
            return false;
        }
        InetAddress localAddress = gateway.getLocalAddress();
        try {
            return reflectAddPortMapping(gateway, port, port, localAddress.getHostAddress(), "TCP", "Minecraft");
        } catch (IOException | SAXException e) {
            LOGGER.error("Failed to add TCP mapping for port {}", port, e);
        }
        return false;
    }

    private static boolean deleteTcpPortMapping(int port) {
        GatewayDevice gateway = getValidGateway();
        if (gateway == null) {
            return false;
        }
        try {
            return gateway.deletePortMapping(port, "TCP");
        } catch (IOException | SAXException e) {
            LOGGER.error("Failed to delete TCP mapping for port {}", port, e);
        }
        return false;
    }

    /**
     * WeUPnP's own {@code addPortMapping} reports only a boolean, which loses the router's error
     * code. Issuing the UPnP command directly lets the code reach the player.
     */
    private static boolean reflectAddPortMapping(GatewayDevice gateway, int externalPort, int internalPort,
                                                 String internalClient, String protocol, String description)
            throws IOException, SAXException {
        Map<String, String> args = new LinkedHashMap<>();
        args.put("NewRemoteHost", "");    // wildcard, any remote host matches
        args.put("NewExternalPort", Integer.toString(externalPort));
        args.put("NewProtocol", protocol);
        args.put("NewInternalPort", Integer.toString(internalPort));
        args.put("NewInternalClient", internalClient);
        args.put("NewEnabled", Integer.toString(1));
        args.put("NewPortMappingDescription", description);
        args.put("NewLeaseDuration", Integer.toString(0));

        String controlURL = (String) getFieldValue(gateway, "controlURL");
        String serviceType = (String) getFieldValue(gateway, "serviceType");
        Map<String, String> nameValue = GatewayDevice.simpleUPnPcommand(controlURL, serviceType, "AddPortMapping", args);

        String errorCode = nameValue.get("errorCode");
        if (errorCode == null) {
            return true;
        }

        ModChat.send(ModChatTranslatableComponent
                .of("chat.open2online.error.code", ModChatTranslatableComponent.MessageTypes.ERROR)
                .append(": " + errorCode));
        return false;
    }

    private static Object getFieldValue(Object fromObject, String fieldName) {
        try {
            Field field = fromObject.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(fromObject);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("Failed to read WeUPnP field {}", fieldName, e);
        }
        return null;
    }
}
