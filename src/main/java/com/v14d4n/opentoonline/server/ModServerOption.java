package com.v14d4n.opentoonline.server;

import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import net.minecraft.client.CycleOption;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// TODO: доделать
@OnlyIn(Dist.CLIENT)
public abstract class ModServerOption {

    public static final CycleOption<Boolean> ALLOW_PVP = CycleOption.createOnOff("options.opentoonline.allowPvp",
            (getter) -> OpenToOnlineConfig.allowPvp.get(),
            (pOptions, pOption, pValue) -> OpenToOnlineConfig.allowPvp.set(pValue)
    );

    // TODO: придумать как по другому можно назвать
    public static final CycleOption<Boolean> LICENSE_REQUIRED = CycleOption.createOnOff("options.opentoonline.licenseRequired",
            (getter) -> OpenToOnlineConfig.licenseRequired.get(),
            (pOptions, pOption, pValue) -> OpenToOnlineConfig.licenseRequired.set(pValue)
    ).setTooltip((pTooltip) -> (p_193636_) -> pTooltip.font.split(new TextComponent("test"), 200));
}