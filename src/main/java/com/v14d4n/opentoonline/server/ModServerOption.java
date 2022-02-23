package com.v14d4n.opentoonline.server;

import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import net.minecraft.client.CycleOption;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ModServerOption {

    public static final CycleOption<EnumLibraries> LIBRARY = CycleOption.create("options.opentoonline.library",
            EnumLibraries.values(),
            EnumLibraries::getTextComponent,
            (getter) -> EnumLibraries.getById(OpenToOnlineConfig.library.get()),
            (a, b, value) -> OpenToOnlineConfig.library.set(value.getId())
    ).setTooltip((pTooltip) -> (p_193636_) -> pTooltip.font.split(new TranslatableComponent("tooltip.opentoonline.library"), 200));

    public static final CycleOption<Boolean> ALLOW_PVP = CycleOption.createOnOff("options.opentoonline.allowPvp",
            (getter) -> OpenToOnlineConfig.allowPvp.get(),
            (pOptions, pOption, pValue) -> OpenToOnlineConfig.allowPvp.set(pValue)
    );

    public static final CycleOption<Boolean> LICENSE_REQUIRED = CycleOption.createOnOff("options.opentoonline.licenseRequired",
            (getter) -> OpenToOnlineConfig.licenseRequired.get(),
            (pOptions, pOption, pValue) -> OpenToOnlineConfig.licenseRequired.set(pValue)
    ).setTooltip((pTooltip) -> (p_193636_) -> pTooltip.font.split(new TranslatableComponent("tooltip.opentoonline.licenseRequired"), 200));
}
