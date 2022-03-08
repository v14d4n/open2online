package com.v14d4n.opentoonline.network;

import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.network.chat.ModChatTranslatableComponent;
import com.v14d4n.opentoonline.network.upnp.IUPnPLibrary;
import com.v14d4n.opentoonline.network.upnp.UPnPLibraries;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.io.IOException;
import java.net.Socket;

import static com.v14d4n.opentoonline.OpenToOnline.minecraft;

public class UPnPHandler {

    private static boolean closePortAfterLogout;
    private static IUPnPLibrary UPnP;

    private static boolean isUPnPAvailable() {
        UPnP = UPnPLibraries.getById(OpenToOnlineConfig.libraryId.get()).getHandler();

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

    public static boolean closePort(int port) {
        if (UPnP == null)
            throw new RuntimeException("UPnP library is not installed");

        minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.closingTcpPort").append(" " + port + "..."));
        if (!UPnP.isMappedTCP(port)) {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.portIsAlreadyClosed"));
        } else if (UPnP.closePortTCP(port)) {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.portIsClosed"));
        } else {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.error.portClosing", ModChatTranslatableComponent.MessageTypes.ERROR));
            return false;
        }

        return true;
    }

    public static void closePortAfterLogout(boolean closePortAfterLogout) {
        UPnPHandler.closePortAfterLogout = closePortAfterLogout;
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        String clientPlayerName = minecraft.getUser().getName();
        String loggedOutPlayerName = event.getPlayer().getName().getString();

        if (clientPlayerName.equals(loggedOutPlayerName) && UPnPHandler.closePortAfterLogout) {
            UPnPHandler.closePortAfterLogout(false);

            int port = OpenToOnlineConfig.port.get();
            if (UPnP.isMappedTCP(port)) {
                UPnP.closePortTCP(port);
            }
        }
    }

    public static boolean isPortAvailable(int port) {
        try (Socket ignored = new Socket("localhost", port)) {
            return false;
        } catch (IOException ignored) {
            return true;
        }
    }

    public static boolean getClosePortAfterLogout() {
        return closePortAfterLogout;
    }
}
