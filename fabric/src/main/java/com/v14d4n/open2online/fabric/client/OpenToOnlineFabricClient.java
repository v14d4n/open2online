package com.v14d4n.open2online.fabric.client;

import com.v14d4n.open2online.OpenToOnline;
import com.v14d4n.open2online.config.OpenToOnlineConfig;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.neoforged.fml.config.ModConfig;

public final class OpenToOnlineFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Fabric has no mod loading stages, so the config is usable right after this call.
        ConfigRegistry.INSTANCE.register(OpenToOnline.MOD_ID, ModConfig.Type.CLIENT,
                OpenToOnlineConfig.SPEC, OpenToOnline.MOD_ID + "-client.toml");

        OpenToOnline.init();
    }
}
