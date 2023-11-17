package com.v14d4n.opentoonline.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;

public class OpenToOnlineConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Integer> port;
    public static final ForgeConfigSpec.ConfigValue<Integer> maxPlayers;
    public static final ForgeConfigSpec.ConfigValue<String> lastIP;
    public static final ForgeConfigSpec.ConfigValue<Boolean> allowPvp;
    public static final ForgeConfigSpec.ConfigValue<Integer> libraryId;
    public static final ForgeConfigSpec.ConfigValue<Boolean> whitelistMode;
    public static final ForgeConfigSpec.ConfigValue<ArrayList<String>> friends;
    public static final ForgeConfigSpec.ConfigValue<Integer> portMapperIndex;
    public static final ForgeConfigSpec.ConfigValue<Boolean> updateNotifications;
    public static final ForgeConfigSpec.ConfigValue<Boolean> hideIP;

    static {
        BUILDER.push("Open2Online config");

        port = BUILDER.comment("Server port. Default value is 25565.").define("Port", 25565);
        maxPlayers = BUILDER.comment("Server max players. Default value is 8.").define("Max Players", 8);
        lastIP = BUILDER.comment("Your last IP. Default value is \"0.0.0.0\".").define("Last IP", "0.0.0.0");
        allowPvp = BUILDER.comment("Allow PVP on the server. Default value is true.").define("Allow PVP", true);
        libraryId = BUILDER.comment("Port opening library. Don't change if everything works fine. 0 - Weupnp; 1 - WaifUPnP; 2 - PortMapper. Default value is 0.").define("Library", 0);
        whitelistMode = BUILDER.comment("Enable whitelist mode. Default value is false.").define("Whitelist", false);
        friends = BUILDER.comment("List of friends who can join the server if the whitelist mode is enabled.").define("FriendList", new ArrayList<>());
        updateNotifications = BUILDER.comment("Chat message about an update.").define("UpdateNotifications", true);
        portMapperIndex = BUILDER.comment("Needed for faster port opening using PortMapper. Don't change it.").define("PortMapperIndex", -1);
        hideIP = BUILDER.comment("Hide your IP.").define("HideIP", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
