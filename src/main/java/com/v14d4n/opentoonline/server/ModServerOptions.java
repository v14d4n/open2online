package com.v14d4n.opentoonline.server;

import com.mojang.serialization.Codec;
import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.network.upnp.UPnPLibraries;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Arrays;

@OnlyIn(Dist.CLIENT)
public abstract class ModServerOptions {

    public static final OptionInstance<UPnPLibraries> LIBRARY = new OptionInstance<>("options.opentoonline.library",
            OptionInstance.cachedConstantTooltip(Component.translatable("tooltip.opentoonline.library")),
            (p_231907_, p_231908_) -> UPnPLibraries.getById(p_231908_.getId()).getTextComponent(),
            new OptionInstance.Enum<>(Arrays.asList(UPnPLibraries.values()), Codec.INT.xmap(UPnPLibraries::byId, UPnPLibraries::getId)),
            UPnPLibraries.getById(OpenToOnlineConfig.libraryId.get()),
            (lib) -> OpenToOnlineConfig.libraryId.set(lib.getId()));


    public static final OptionInstance<Boolean> ALLOW_PVP = OptionInstance.createBoolean("options.opentoonline.allowPvp",
            OpenToOnlineConfig.allowPvp.get(),
            OpenToOnlineConfig.allowPvp::set
    );

    public static final OptionInstance<Boolean> WHITELIST_MODE = OptionInstance.createBoolean("gui.opentoonline.whitelistMode",
            OptionInstance.cachedConstantTooltip(Component.translatable("tooltip.opentoonline.whitelist")),
            OpenToOnlineConfig.whitelistMode.get(),
            OpenToOnlineConfig.whitelistMode::set
    );

    // TODO: fix friendlist. They made OptionInstance class final >-<*
//    public static final OptionInstance EDIT_WHITELIST = new OptionInstance(
//            "gui.opentoonline.editWhitelist",
//            OptionInstance.noTooltip(),
//            (p_231581_, p_231582_) -> Component.literal(""),
//            new OptionInstance.Enum<>(ImmutableList.of(Boolean.FALSE, Boolean.TRUE), Codec.BOOL),
//            Codec.EMPTY.codec(),
//            false,
//            (a) -> minecraft.setScreen(new EditWhitelistScreen(minecraft.screen))) {
//        @Override
//        public AbstractWidget createButton(Options p_231508_, int p_231509_, int p_231510_, int p_231511_) {
//            return super.createButton(p_231508_, p_231509_, p_231510_, p_231511_);
//        }
//    };
}
