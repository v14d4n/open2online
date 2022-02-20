package com.v14d4n.opentoonline.network;

import com.dosse.upnp.UPnP;
import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.network.chat.ModChatTranslatableComponent;
import net.minecraft.network.chat.TextComponent;

import static com.v14d4n.opentoonline.OpenToOnline.minecraft;

public class UPnPHandler {

    private static boolean isUPnPAvailable() {
        if (UPnP.isUPnPAvailable()) {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.upnpIsAvailable"));
            return true;
        } else {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.error.upnpIsNotAvailable", ModChatTranslatableComponent.MessageTypes.ERROR));
            return false;
        }
    }

    public static boolean openPort(int port) {
        if (!isUPnPAvailable())
            return false;

        minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.openingTcpPort").append(" " + port + "..."));

        if(UPnP.isMappedTCP(port)) {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.portIsAlreadyOpen"));
        } else if (UPnP.openPortTCP(port)){
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.portIsOpen"));
        } else {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.error.portOpening", ModChatTranslatableComponent.MessageTypes.ERROR));
            return false;
        }
        OpenToOnlineConfig.port.set(port);
        return true;
    }

    public static int closePort(int port) {
        minecraft.gui.getChat().addMessage(new TextComponent("Closing TCP port " + port + "..."));

        if (!UPnP.isMappedTCP(port)) {
            minecraft.gui.getChat().addMessage(new TextComponent("Port is already closed"));
        } else if (UPnP.closePortTCP(port)) {
            minecraft.gui.getChat().addMessage(new TextComponent("Port is closed"));
        } else {
            minecraft.gui.getChat().addMessage(new TextComponent("Some error occurred while closing the port."));
        }

        return 1;
    }
}
