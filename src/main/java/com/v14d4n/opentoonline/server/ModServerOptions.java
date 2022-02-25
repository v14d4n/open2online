package com.v14d4n.opentoonline.server;

import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import net.minecraft.client.CycleOption;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ModServerOptions {

    private static int libraryId;
    private static boolean allowPvp;
    private static boolean licenseRequired;

    public static final CycleOption<EnumLibraries> LIBRARY = CycleOption.create("options.opentoonline.library",
            EnumLibraries.values(),
            EnumLibraries::getTextComponent,
            (getter) ->  {
                libraryId = OpenToOnlineConfig.library.get();
                return EnumLibraries.getById(libraryId);
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

    public static final CycleOption<Boolean> LICENSE_REQUIRED = CycleOption.createOnOff("options.opentoonline.licenseRequired",
            (getter) -> {
                licenseRequired = OpenToOnlineConfig.licenseRequired.get();
                return licenseRequired;
            },
            (pOptions, pOption, pValue) -> licenseRequired = pValue
    ).setTooltip((pTooltip) -> (p_193636_) -> pTooltip.font.split(new TranslatableComponent("tooltip.opentoonline.licenseRequired"), 200));

    public static void save() {
        OpenToOnlineConfig.library.set(libraryId);
        OpenToOnlineConfig.allowPvp.set(allowPvp);
        OpenToOnlineConfig.licenseRequired.set(licenseRequired);
    }
}
