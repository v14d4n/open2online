package com.v14d4n.opentoonline.network;

import com.mojang.authlib.GameProfile;
import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.network.chat.ModChatTranslatableComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.resources.I18n;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;

import static com.v14d4n.opentoonline.OpenToOnline.minecraft;

@OnlyIn(Dist.CLIENT)
public class ServerHandler {

    public static boolean startServer(int port, int maxPlayers, GameType gameMode, boolean allowCommands, boolean online) {
        if (online && !UPnPHandler.openPort(port))
            return false;

        if (minecraft.getSingleplayerServer().publishServer(gameMode, allowCommands, port)) {
            setupServerConfiguration(maxPlayers, online);
            printHostedGameMessage(online, port);
        } else {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.error.publishFailed", ModChatTranslatableComponent.MessageTypes.ERROR));
            UPnPHandler.closePort(port);
            return false;
        }

        return true;
    }

    private static void printHostedGameMessage(boolean online, int port) {
        if (online) {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.gameHostedOn").append(getServerFormattedAddress(port)));
        } else {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.localGameHostedOn").append(getServerFormattedPort(port)));
        }
    }

    private static void setupServerConfiguration(int maxPlayers, boolean closePortAfterLogout) {
        UPnPHandler.closePortAfterLogout(closePortAfterLogout);
        ServerHandler.setMaxPlayers(maxPlayers);

        ServerHandler.setPvpAllowed(OpenToOnlineConfig.allowPvp.get());
    }

    public static boolean isServerPublished() {
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
            Field field = PlayerList.class.getDeclaredField("field_72405_c"); // Works only in build version
            //Field field = PlayerList.class.getDeclaredField("maxPlayers"); // Works only if you run client from IDE
            field.setAccessible(true);
            field.setInt(playerList, maxPlayers);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.warn.settingMaxPlayers", ModChatTranslatableComponent.MessageTypes.WARN));
            return false;
        }

        OpenToOnlineConfig.maxPlayers.set(maxPlayers);
        return true;
    }

    private static String getExternalIP() {
        try {
            String currentIP = new BufferedReader(new InputStreamReader(new URL("https://checkip.amazonaws.com").openStream())).readLine();
            String lastIP = OpenToOnlineConfig.lastIP.get();

            // 0.0.0.0 is a default value
            if (!currentIP.equals(lastIP) && !lastIP.equals("0.0.0.0")) {
                minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.ipIsChanged", ModChatTranslatableComponent.MessageTypes.WARN));
            }
            OpenToOnlineConfig.lastIP.set(currentIP);
            return currentIP;
        } catch (IOException e) {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.warn.gettingAnExternalIP", ModChatTranslatableComponent.MessageTypes.WARN));
            return "0.0.0.0";
        }
    }

    private static IFormattableTextComponent getServerFormattedPort(int port) {
        String stringPort = String.valueOf(port);
        IFormattableTextComponent formattablePort = new StringTextComponent(stringPort).setStyle(Style.EMPTY.setUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, stringPort))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent("tooltip.opentoonline.copy"))));

        return new StringTextComponent(" [").append(formattablePort).append("]");
    }

    private static IFormattableTextComponent getServerFormattedAddress(int port) {
        String stringAddress = getExternalIP() + ":" + port;
        IFormattableTextComponent serverAddress = new StringTextComponent(stringAddress).setStyle(Style.EMPTY.setUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, stringAddress))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent("tooltip.opentoonline.copy"))));

        return new StringTextComponent(" [").append(serverAddress).append("]");
    }

    public static boolean isPlayerServerOwner(GameProfile gameProfile) {
        return minecraft.getSingleplayerServer().isSingleplayerOwner(gameProfile);
    }

    public static boolean isClientRunningOnlineServer() {
        return UPnPHandler.getClosePortAfterLogout();
    }
}
