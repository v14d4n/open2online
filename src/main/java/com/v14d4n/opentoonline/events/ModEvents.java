package com.v14d4n.opentoonline.events;

import com.v14d4n.opentoonline.OpenToOnline;
import com.v14d4n.opentoonline.commands.*;
import com.v14d4n.opentoonline.network.UPnPHandler;
import com.v14d4n.opentoonline.network.chat.ModChatTranslatableComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.player.Player;
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
    private static final String clientPlayerName = minecraft.getUser().getName();

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
        // TODO: добавить уведомление, о том что мод находится в разработке
        String loggedInPlayerName = event.getPlayer().getName().getString();
        if (clientPlayerName.equals(loggedInPlayerName)) {
            checkUpdates(event.getPlayer());
        }
    }

    private static void checkUpdates(Player player) {
        IModInfo modInfo = ModList.get().getModContainerById(OpenToOnline.MOD_ID).get().getModInfo();
        VersionChecker.CheckResult updateCheckResult = VersionChecker.getResult(modInfo);

        if (updateCheckResult.status().equals(BETA_OUTDATED)) {
            String currentVersion = modInfo.getVersion().toString().substring(modInfo.getVersion().toString().lastIndexOf('-') + 1);
            String actualVersion = updateCheckResult.target().toString().substring(updateCheckResult.target().toString().lastIndexOf('-') + 1);

            String mainText = new ModChatTranslatableComponent("chat.opentoonline.update", ModChatTranslatableComponent.MessageTypes.WARN).getString() + " \u00A7c" + currentVersion + "\u00A7r -> \u00A7a" + actualVersion + "\u00A7r";
            MutableComponent link = new TranslatableComponent("chat.opentoonline.link")
                    .setStyle(Style.EMPTY.setUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, updateCheckResult.url()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("chat.opentoonline.openUrl"))));

            MutableComponent message = new TextComponent(mainText).append(" [").append(link).append("]");

            player.sendMessage(message, UUID.randomUUID());
        }
    }

}
