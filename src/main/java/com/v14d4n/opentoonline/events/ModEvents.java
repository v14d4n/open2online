package com.v14d4n.opentoonline.events;

import com.v14d4n.opentoonline.OpenToOnline;
import com.v14d4n.opentoonline.commands.OpenToOnlineCommand;
import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.network.ServerHandler;
import com.v14d4n.opentoonline.network.UPnPHandler;
import com.v14d4n.opentoonline.network.chat.ModChatTranslatableComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.server.command.ConfigCommand;

import static net.minecraftforge.fml.VersionChecker.Status.BETA_OUTDATED;

@Mod.EventBusSubscriber(modid = OpenToOnline.MOD_ID)
public class ModEvents {

    private static final Minecraft minecraft = Minecraft.getInstance();
    private static final User clientPlayer = minecraft.getUser();

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        new OpenToOnlineCommand(event.getDispatcher());

        ConfigCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UPnPHandler.onPlayerLoggedOut(event);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        String loggedInPlayerName = event.getEntity().getName().getString();
        if (clientPlayer.getName().equals(loggedInPlayerName)) {
            checkUpdates(event.getEntity());
        }

        // runs only on the host side
        if (ServerHandler.isClientRunningOnlineServer() && ServerHandler.isPlayerServerOwner(clientPlayer.getGameProfile())) {

            if (OpenToOnlineConfig.whitelistMode.get()) {
                kickNotWhitelistedPlayer(event.getEntity());
            }

        }

    }

    private static void kickNotWhitelistedPlayer(Player player) {
        if (clientPlayer.getName().equals(player.getName().getString()))
            return;

        for (String friendName : OpenToOnlineConfig.friends.get()) {
            if (friendName.equals(player.getName().getString())) {
                return;
            }
        }
        ((ServerPlayer) player).connection.disconnect(Component.literal("Not in the whitelist"));
    }

    private static void checkUpdates(Player player) {
        IModInfo modInfo = ModList.get().getModContainerById(OpenToOnline.MOD_ID).get().getModInfo();
        VersionChecker.CheckResult updateCheckResult = VersionChecker.getResult(modInfo);

        if (updateCheckResult.status().equals(BETA_OUTDATED)) {
            String currentVersion = modInfo.getVersion().toString().substring(modInfo.getVersion().toString().lastIndexOf('-') + 1);
            String actualVersion = updateCheckResult.target().toString().substring(updateCheckResult.target().toString().lastIndexOf('-') + 1);

            String mainText = ModChatTranslatableComponent.getTranslatableComponentWithPrefix("chat.opentoonline.update", ModChatTranslatableComponent.MessageTypes.WARN).getString() + " \u00A7c" + currentVersion + "\u00A7r -> \u00A7a" + actualVersion + "\u00A7r";
            MutableComponent link = Component.translatable("chat.opentoonline.link")
                    .setStyle(Style.EMPTY.withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, updateCheckResult.url()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("tooltip.opentoonline.openUrl"))));

            MutableComponent message = Component.literal(mainText).append(" [").append(link).append("]");

            // TODO: Если сообщение выводится для нескольких клиентов при входе
            minecraft.gui.getChat().addMessage(message);
        }
    }

}
