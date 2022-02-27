package com.v14d4n.opentoonline.server;

import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.network.upnp.UPnPLibraries;
import net.minecraft.client.CycleOption;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ModServerOptions {

    private static int libraryId;
    private static boolean allowPvp;
    private static boolean whitelistMode;

    public static final CycleOption<UPnPLibraries> LIBRARY = CycleOption.create("options.opentoonline.library",
            UPnPLibraries.values(),
            UPnPLibraries::getTextComponent,
            (getter) ->  {
                libraryId = OpenToOnlineConfig.libraryId.get();
                return UPnPLibraries.getById(libraryId);
            },
            (a, b, library) -> libraryId = library.getId()
    ).setTooltip((pTooltip) -> (p_193636_) -> pTooltip.font.split(new TranslatableComponent("tooltip.opentoonline.library"), 200));

    public static final CycleOption<Boolean> ALLOW_PVP = CycleOption.createOnOff("options.opentoonline.allowPvp",
            (getter) -> {
                allowPvp = OpenToOnlineConfig.allowPvp.get();
                return allowPvp;
            },
            (pOptions, pOption, pValue) -> allowPvp = pValue
    );

    public static final CycleOption<Boolean> WHITELIST_MODE = CycleOption.createOnOff("gui.opentoonline.whitelistMode",
            (getter) -> {
                whitelistMode = OpenToOnlineConfig.whitelistMode.get();
                return whitelistMode;
            },
            (pOptions, pOption, pValue) -> whitelistMode = pValue
    );

    public static void save() {
        OpenToOnlineConfig.libraryId.set(libraryId);
        OpenToOnlineConfig.allowPvp.set(allowPvp);
        OpenToOnlineConfig.whitelistMode.set(whitelistMode);
    }
}
