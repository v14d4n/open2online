package com.v14d4n.opentoonline;

import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(OpenToOnline.MOD_ID)
public class OpenToOnline {
    public static final String MOD_ID = "opentoonline";
    public static final Minecraft minecraft = Minecraft.getInstance();

    public OpenToOnline() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, OpenToOnlineConfig.SPEC, "opentoonline-client.toml");
        MinecraftForge.EVENT_BUS.register(this);
    }
    // TODO: сменить лого и описание мода
    private void setup(final FMLCommonSetupEvent event) {

    }
}
