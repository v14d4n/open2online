package com.v14d4n.opentoonline.server;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.network.upnp.UPnPLibraries;
import com.v14d4n.opentoonline.screens.EditWhitelistScreen;
import net.minecraft.client.*;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.util.OptionEnum;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Arrays;

import static com.v14d4n.opentoonline.OpenToOnline.minecraft;

@OnlyIn(Dist.CLIENT)
public abstract class ModServerOptions {

    public static final OptionInstance<UPnPLibraries> LIBRARY = new OptionInstance<>("options.opentoonline.library",
            OptionInstance.cachedConstantTooltip(Component.translatable("tooltip.opentoonline.library")),
            (p_231907_, p_231908_) -> UPnPLibraries.getById(p_231908_.getId()).getTextComponent(),
            new OptionInstance.Enum<>(Arrays.asList(UPnPLibraries.values()), Codec.INT.xmap(UPnPLibraries::byId, UPnPLibraries::getId)),
            UPnPLibraries.getById(OpenToOnlineConfig.libraryId.get()),
            (lib) -> OpenToOnlineConfig.libraryId.set(lib.getId()));

//    public static final OptionInstance<UPnPLibraries> LIBRARY = OptionInstance.create("options.opentoonline.library",
//            UPnPLibraries.values(),
//            UPnPLibraries::getTextComponent,
//            (getter) ->  {
//                libraryId = OpenToOnlineConfig.libraryId.get();
//                return UPnPLibraries.getById(libraryId);
//            },
//            (a, b, library) -> libraryId = library.getId()
//    ).setTooltip((pTooltip) -> (p_193636_) -> pTooltip.font.split(Component.translatable("tooltip.opentoonline.library"), 200));

    public static final OptionInstance<Boolean> ALLOW_PVP = OptionInstance.createBoolean("options.opentoonline.allowPvp",
            OpenToOnlineConfig.allowPvp.get(),
            OpenToOnlineConfig.allowPvp::set
    );

    public static final OptionInstance<Boolean> WHITELIST_MODE = OptionInstance.createBoolean("gui.opentoonline.whitelistMode",
            OpenToOnlineConfig.whitelistMode.get(),
            OpenToOnlineConfig.whitelistMode::set
    );

//    public static final OptionInstance EDIT_WHITELIST = new OptionInstance(
//            "gui.opentoonline.editWhitelist",
//            OptionInstance.noTooltip(),
//            OptionInstance.forOptionEnum()) {
//    };
}
