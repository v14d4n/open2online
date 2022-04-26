package com.v14d4n.opentoonline.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.network.ServerHandler;
import com.v14d4n.opentoonline.network.UPnPHandler;
import com.v14d4n.opentoonline.network.chat.ModChatTranslatableComponent;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.world.GameType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static com.v14d4n.opentoonline.OpenToOnline.minecraft;

public class OpenToOnlineCommand {

    public OpenToOnlineCommand(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("open2online").executes((command) -> open()));
    }

    public static int open() {
        int port = OpenToOnlineConfig.port.get();
        int maxPlayers = OpenToOnlineConfig.maxPlayers.get();
        GameType gameMode = minecraft.gameMode.getPlayerMode();
        boolean allowCheats = minecraft.player.hasPermissions(4);

        return open(port, maxPlayers, gameMode, allowCheats);
    }

    public static int open(int port, int maxPlayers, GameType gameMode, boolean allowCommands) {

        if (ServerHandler.isServerPublished()) {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.error.serverIsAlreadyPublished", ModChatTranslatableComponent.MessageTypes.ERROR));
            return 0;
        }

        if (!UPnPHandler.isPortAvailable(port)) {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.error.publishFailed", ModChatTranslatableComponent.MessageTypes.ERROR));
            return 0;
        }

        minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.startingServer"));
        if (!ServerHandler.startServer(port, maxPlayers, gameMode, allowCommands)) {
            return 0;
        }

        return 1;
    }
}
