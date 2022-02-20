package com.v14d4n.opentoonline.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class OpenToOnlineConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Integer> port;
    public static final ForgeConfigSpec.ConfigValue<Integer> maxPlayers;
    public static final ForgeConfigSpec.ConfigValue<String> lastIP;

    static {
        BUILDER.push("Open2Online config");

        port = BUILDER.comment("Server port. Default value is 25565.").define("Port", 25565);
        maxPlayers = BUILDER.comment("Server max players. Default value is 8.").define("Max Players", 8);
        lastIP = BUILDER.comment("Your last IP.").define("Last IP", "0.0.0.0");

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
