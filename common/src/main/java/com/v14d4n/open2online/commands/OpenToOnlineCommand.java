package com.v14d4n.open2online.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.network.ServerHandler;
import com.v14d4n.open2online.network.UPnPHandler;
import com.v14d4n.open2online.network.chat.ModChat;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent.MessageTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.level.GameType;

@Environment(EnvType.CLIENT)
public final class OpenToOnlineCommand {
    private OpenToOnlineCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("open2online").executes(context -> open()));
    }

    public static int open() {
        Minecraft minecraft = Minecraft.getInstance();
        int port = OpenToOnlineConfig.port.get();
        int maxPlayers = OpenToOnlineConfig.maxPlayers.get();
        GameType gameMode = minecraft.gameMode != null ? minecraft.gameMode.getPlayerMode() : GameType.SURVIVAL;
        boolean allowCheats = ServerHandler.canLocalPlayerUseCheats();

        return open(port, maxPlayers, gameMode, allowCheats, true);
    }

    public static int open(int port, int maxPlayers, GameType gameMode, boolean allowCommands, boolean online) {
        ModChat.send(ModChatTranslatableComponent.of("chat.open2online.startingServer"));

        if (ServerHandler.isServerPublished()) {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.error.serverIsAlreadyPublished", MessageTypes.ERROR));
            return 0;
        }

        if (online && !UPnPHandler.isPortAvailable(port)) {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.error.publishFailed", MessageTypes.ERROR));
            return 0;
        }

        return ServerHandler.startServer(port, maxPlayers, gameMode, allowCommands, online) ? 1 : 0;
    }
}
