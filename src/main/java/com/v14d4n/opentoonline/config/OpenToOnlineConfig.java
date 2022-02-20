package com.v14d4n.opentoonline.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class OpenToOnlineConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Integer> port;
    public static final ForgeConfigSpec.ConfigValue<Integer> maxPlayers;
    public static final ForgeConfigSpec.ConfigValue<String> lastIP;
    public static final ForgeConfigSpec.ConfigValue<Boolean> allowPvp;
    public static final ForgeConfigSpec.ConfigValue<Boolean> licenseRequired;

    static {
        BUILDER.push("Open2Online config");

        port = BUILDER.comment("Server port. Default value is 25565.").define("Port", 25565);
        maxPlayers = BUILDER.comment("Server max players. Default value is 8.").define("Max Players", 8);
        lastIP = BUILDER.comment("Your last IP. Default value is \"0.0.0.0\".").define("Last IP", "0.0.0.0");
        allowPvp = BUILDER.comment("Allow PVP on the server. Default value is true.").define("Allow PVP", true);
        licenseRequired = BUILDER.comment("Require a licensed version of the game to connect. Default value is false.").define("License Required", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}