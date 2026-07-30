package com.v14d4n.open2online.neoforge;

import com.v14d4n.open2online.OpenToOnline;
import com.v14d4n.open2online.config.OpenToOnlineConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(OpenToOnline.MOD_ID)
public final class OpenToOnlineNeoForge {
    public OpenToOnlineNeoForge(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, OpenToOnlineConfig.SPEC,
                OpenToOnline.MOD_ID + "-client.toml");

        OpenToOnline.init();
    }
}
