package com.v14d4n.open2online.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.network.PublishTask;
import com.v14d4n.open2online.network.ServerHandler;
import com.v14d4n.open2online.network.UPnPHandler;
import com.v14d4n.open2online.network.chat.ModChat;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent;
import com.v14d4n.open2online.network.chat.ModChatTranslatableComponent.MessageTypes;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent.ClientCommandSourceStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.GameType;

@Environment(EnvType.CLIENT)
public final class OpenToOnlineCommand {
    private OpenToOnlineCommand() {
    }

    /**
     * Registered on the <em>client</em> dispatcher, and that is the whole point.
     *
     * <p>Everything below acts on {@code Minecraft.getInstance()}, which on a hosted world is the
     * host's own client. Put on the server dispatcher — where 1.16.5 had it — the command is offered
     * to every player in the world, so any guest could push the host's machine into publishing to the
     * internet and mapping a port on the host's router. A client command never reaches the server, so
     * only the person at the keyboard can run it.
     */
    public static void register(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        dispatcher.register(ClientCommandRegistrationEvent.literal("open2online")
                .executes(context -> openInBackground()));
    }

    /**
     * Hands the work to a thread of its own: the command runs on the client thread, and mapping a
     * port takes seconds at best. This is the same worker the button and the auto start use.
     */
    private static int openInBackground() {
        Minecraft minecraft = Minecraft.getInstance();
        int port = OpenToOnlineConfig.port.get();
        int maxPlayers = OpenToOnlineConfig.maxPlayers.get();
        GameType gameMode = minecraft.gameMode != null ? minecraft.gameMode.getPlayerMode() : GameType.SURVIVAL;
        boolean allowCheats = ServerHandler.canLocalPlayerUseCheats();

        Thread worker = new Thread(() -> open(port, maxPlayers, gameMode, allowCheats, true),
                "Open2Online publish");
        worker.setDaemon(true);
        worker.start();
        return 1;
    }

    public static int open(int port, int maxPlayers, GameType gameMode, boolean allowCommands, boolean online) {
        // One publish at a time: the command can be run again while the first is still working.
        if (!PublishTask.begin()) {
            ModChat.send(ModChatTranslatableComponent.of("chat.open2online.error.alreadyStarting", MessageTypes.ERROR));
            return 0;
        }

        try {
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
        } finally {
            PublishTask.finish();
        }
    }
}
