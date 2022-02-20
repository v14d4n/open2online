package com.v14d4n.opentoonline.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.network.ServerHandler;
import com.v14d4n.opentoonline.network.chat.ModChatTranslatableComponent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.level.GameType;

import static com.v14d4n.opentoonline.OpenToOnline.minecraft;

public class OpenToOnlineCommand {

    public OpenToOnlineCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("open2online").executes((command) -> open()));
    }

    public static int open() {
        int port = OpenToOnlineConfig.port.get();
        int maxPlayers = OpenToOnlineConfig.maxPlayers.get();
        GameType gameMode = minecraft.gameMode.getPlayerMode();
        boolean allowCheats = (minecraft.player.getPermissionLevel() == 4);
        return open(port, maxPlayers, gameMode, allowCheats);
    }

    public static int open(int port, int maxPlayers, GameType gameMode, boolean allowCommands) {

        if (ServerHandler.isServerAlreadyPublished()) {
            minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.error.serverIsAlreadyPublished", ModChatTranslatableComponent.MessageTypes.ERROR));
            return 0;
        }

        minecraft.gui.getChat().addMessage(new ModChatTranslatableComponent("chat.opentoonline.startingServer"));
        if (!ServerHandler.startServer(port, maxPlayers, gameMode, allowCommands)) {
            return 0;
        }

        return 1;
    }
}
