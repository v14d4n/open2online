package com.v14d4n.opentoonline.events;

import com.v14d4n.opentoonline.OpenToOnline;
import com.v14d4n.opentoonline.commands.*;
import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.network.ServerHandler;
import com.v14d4n.opentoonline.network.UPnPHandler;
import com.v14d4n.opentoonline.network.chat.ModChatTranslatableComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Session;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.server.command.ConfigCommand;

import java.util.UUID;

import static net.minecraftforge.fml.VersionChecker.Status.BETA_OUTDATED;

@Mod.EventBusSubscriber(modid = OpenToOnline.MOD_ID)
public class ModEvents {

    private static final Minecraft minecraft = Minecraft.getInstance();
    private static final Session clientPlayer = minecraft.getUser();

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
        String loggedInPlayerName = event.getPlayer().getName().getString();
        if (clientPlayer.getName().equals(loggedInPlayerName)) {
            checkUpdates(event.getPlayer());
        }

        // runs only on the host side
        if (ServerHandler.isClientRunningOnlineServer() && ServerHandler.isPlayerServerOwner(clientPlayer.getGameProfile())) {

            if (OpenToOnlineConfig.whitelistMode.get()) {
                kickNotWhitelistedPlayer(event.getPlayer());
            }

        }

    }

    private static void kickNotWhitelistedPlayer(PlayerEntity player) {
        if (clientPlayer.getName().equals(player.getName().getString()))
            return;

        for (String friendName : OpenToOnlineConfig.friends.get()) {
            if (friendName.equals(player.getName().getString())){
                return;
            }
        }
        ((ServerPlayerEntity)player).connection.disconnect(new StringTextComponent("Not in the whitelist"));
    }

    private static void checkUpdates(PlayerEntity player) {
        if (!OpenToOnlineConfig.updateNotifications.get()) return;

        IModInfo modInfo = ModList.get().getModContainerById(OpenToOnline.MOD_ID).get().getModInfo();
        VersionChecker.CheckResult updateCheckResult = VersionChecker.getResult(modInfo);

        if (updateCheckResult.status.equals(BETA_OUTDATED)) {
            String currentVersion = modInfo.getVersion().toString().substring(modInfo.getVersion().toString().lastIndexOf('-') + 1);
            String actualVersion = updateCheckResult.target.toString().substring(updateCheckResult.target.toString().lastIndexOf('-') + 1);

            String mainText = new ModChatTranslatableComponent("chat.opentoonline.update", ModChatTranslatableComponent.MessageTypes.WARN).getString() + " \u00A7c" + currentVersion + "\u00A7r -> \u00A7a" + actualVersion + "\u00A7r";
            IFormattableTextComponent link = new TranslationTextComponent("chat.opentoonline.link")
                    .setStyle(Style.EMPTY.setUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, updateCheckResult.url))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent("tooltip.opentoonline.openUrl"))));

            IFormattableTextComponent message = new StringTextComponent(mainText).append(" [").append(link).append("]");

            player.sendMessage(message, UUID.randomUUID());
        }
    }

}
