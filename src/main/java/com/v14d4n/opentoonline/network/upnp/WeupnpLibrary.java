package com.v14d4n.opentoonline.network.upnp;

import net.minecraft.network.chat.TextComponent;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;

import static com.v14d4n.opentoonline.OpenToOnline.minecraft;

public class WeupnpLibrary implements IUPnPLibrary {
    @Override
    public boolean isUPnPAvailable() {
        try {
            GatewayDiscover discover = new GatewayDiscover();
            discover.discover();
            GatewayDevice d = discover.getValidGateway();
            minecraft.gui.getChat().addMessage(new TextComponent(d.getExternalIPAddress()));
        } catch (Exception e) {
            minecraft.gui.getChat().addMessage(new TextComponent(e.getMessage()));
        }

        return false;
    }

    @Override
    public boolean isMappedTCP(int port) {
        return false;
    }

    @Override
    public boolean openPortTCP(int port) {
        return false;
    }

    @Override
    public boolean closePortTCP(int port) {
        return false;
    }
}
