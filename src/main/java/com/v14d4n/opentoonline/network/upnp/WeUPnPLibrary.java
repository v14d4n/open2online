package com.v14d4n.opentoonline.network.upnp;

import com.v14d4n.opentoonline.network.chat.ModChatTranslatableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.v14d4n.opentoonline.OpenToOnline.minecraft;

public class WeUPnPLibrary implements IUPnPLibrary {

    private static final Logger LOGGER = LogManager.getLogger();
    private static GatewayDevice gatewayDevice;
    private static PortMappingEntry portMapping;

    @Override
    public boolean isUPnPAvailable() {
        return WeUPnPLibrary.getValidGateway() != null;
    }

    @Override
    public boolean isMappedTCP(int port) {
        return WeUPnPLibrary.getTcpPortMapping(port);
    }

    @Override
    public boolean openPortTCP(int port) {
        return WeUPnPLibrary.addTcpPortMapping(port);
    }

    @Override
    public boolean closePortTCP(int port) {
        return WeUPnPLibrary.deleteTcpPortMapping(port);
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
            return WeUPnPLibrary.getValidGateway().getSpecificPortMappingEntry(port, "TCP", WeUPnPLibrary.getPortMappingEntry());
        } catch (IOException | SAXException e) {
            LOGGER.error("[Open2Online-Log]: "  + e.getMessage());
        }
        return false;
    }

    private static boolean addTcpPortMapping(int port) {
        InetAddress localAddress = WeUPnPLibrary.getValidGateway().getLocalAddress();
        try {
            return WeUPnPLibrary.reflectAddPortMapping(getValidGateway(), port, port, localAddress.getHostAddress(), "TCP", "Minecraft");
        } catch (IOException | SAXException e) {
            LOGGER.error("[Open2Online-Log]: "  + e.getMessage());
        }
        return false;
    }

    private static boolean deleteTcpPortMapping(int port) {
        try {
            return WeUPnPLibrary.getValidGateway().deletePortMapping(port, "TCP");
        } catch (IOException | SAXException e) {
            LOGGER.error("[Open2Online-Log]: "  + e.getMessage());
        }
        return false;
    }

    private static boolean reflectAddPortMapping(GatewayDevice gatewayDevice, int externalPort, int internalPort, String internalClient, String protocol, String description) throws IOException, SAXException {
        Map<String, String> args = new LinkedHashMap<String, String>();
        args.put("NewRemoteHost", "");    // wildcard, any remote host matches
        args.put("NewExternalPort", Integer.toString(externalPort));
        args.put("NewProtocol", protocol);
        args.put("NewInternalPort", Integer.toString(internalPort));
        args.put("NewInternalClient", internalClient);
        args.put("NewEnabled", Integer.toString(1));
        args.put("NewPortMappingDescription", description);
        args.put("NewLeaseDuration", Integer.toString(0));

        String controlURL = (String) getFieldValue(getValidGateway(), "controlURL");
        String serviceType= (String) getFieldValue(getValidGateway(), "serviceType");
        Map<String, String> nameValue = GatewayDevice.simpleUPnPcommand(controlURL, serviceType, "AddPortMapping", args);

        String errorCode = nameValue.get("errorCode");
        if (errorCode == null) {
            return true;
        } else {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.error.code", ModChatTranslatableComponent.MessageTypes.ERROR).append(": " + errorCode));
            return false;
        }
    }

    private static Object getFieldValue(Object fromObject, String fieldName) {
        try {
            Field field = fromObject.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(fromObject);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
