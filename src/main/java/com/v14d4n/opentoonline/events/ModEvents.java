package com.v14d4n.opentoonline.events;

import com.dosse.upnp.UPnP;
import com.v14d4n.opentoonline.OpenToOnline;
import com.v14d4n.opentoonline.commands.*;
import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;

@Mod.EventBusSubscriber(modid = OpenToOnline.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        new OpenToOnlineCommand(event.getDispatcher());

        ConfigCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isClientSide()) {
            int port = OpenToOnlineConfig.port.get();
            if (UPnP.isMappedTCP(port)){
                UPnP.closePortTCP(port);
            }
        }
    }

}
