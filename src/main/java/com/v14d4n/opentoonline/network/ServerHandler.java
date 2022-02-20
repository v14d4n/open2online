package com.v14d4n.opentoonline.network;

import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.network.chat.ModChatTranslatableComponent;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;

import static com.v14d4n.opentoonline.OpenToOnline.minecraft;

public class ServerHandler {

    public static boolean startServer(int port, int maxPlayers, GameType gameMode, boolean allowCommands) {
        if (!UPnPHandler.openPort(port))
            return false;

        if (minecraft.getSingleplayerServer().publishServer(gameMode, allowCommands, port)) {
            ServerHandler.setMaxPlayers(maxPlayers);
            ServerHandler.setPvpAllowed(true);
            ServerHandler.setLicenseGameRequired(false);
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.gameHostedOn").append(getServerFormattedAddress(port)));
        } else {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.error.publishFailed", ModChatTranslatableComponent.MessageTypes.ERROR));
            UPnPHandler.closePort(port);
            return false;
        }

        return true;
    }

    public static boolean isServerAlreadyPublished() {
        return !minecraft.hasSingleplayerServer() || minecraft.getSingleplayerServer().isPublished();
    }

    private static void setLicenseGameRequired(boolean requireLicense) {
        minecraft.getSingleplayerServer().setUsesAuthentication(requireLicense);
    }

    private static void setPvpAllowed(boolean allowPvp) {
        minecraft.getSingleplayerServer().setPvpAllowed(allowPvp);
    }

    private static boolean setMaxPlayers(int maxPlayers) {
        PlayerList playerList = minecraft.getSingleplayerServer().getPlayerList();
        try {
            Field field = PlayerList.class.getDeclaredField("f_11193_"); // Works only in build version
            //Field field = PlayerList.class.getDeclaredField("maxPlayers"); // Works only if you run client from IDE
            field.setAccessible(true);
            field.setInt(playerList, maxPlayers);
            OpenToOnlineConfig.maxPlayers.set(maxPlayers);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.warn.settingMaxPlayers", ModChatTranslatableComponent.MessageTypes.WARN));
            return false;
        }

        return true;
    }

    private static String getExternalIP() {
        try {
            String currentIP = new BufferedReader(new InputStreamReader(new URL("https://checkip.amazonaws.com").openStream())).readLine();
            String lastIP = OpenToOnlineConfig.lastIP.get();

            // 0.0.0.0 is a default value
            if (!currentIP.equals(lastIP) && !lastIP.equals("0.0.0.0")){
                minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.ipIsChanged", ModChatTranslatableComponent.MessageTypes.WARN));
            }

            OpenToOnlineConfig.lastIP.set(currentIP);
            return currentIP;
        } catch (IOException e) {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.warn.gettingAnExternalIP", ModChatTranslatableComponent.MessageTypes.WARN));
            return "0.0.0.0";
        }
    }

    private static MutableComponent getServerFormattedAddress(int port) {
        String stringAddress = getExternalIP() + ":" + port;
        MutableComponent serverAddress = new TextComponent(stringAddress).setStyle(Style.EMPTY.setUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, stringAddress))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("gui.opentoonline.copy"))));

        return new TextComponent(" [").append(serverAddress).append("]");
    }
}
