package com.v14d4n.open2online.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;

/**
 * Config layout carried over from the 1.16.5 Forge build.
 *
 * <p>{@code ForgeConfigSpec} became {@code ModConfigSpec} under NeoForge; on Fabric the very same
 * class is supplied by Forge Config API Port, so this stays in the common module. Registration is
 * per-loader and lives in the platform entrypoints.
 */
public final class OpenToOnlineConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<Integer> port;
    public static final ModConfigSpec.ConfigValue<Integer> maxPlayers;
    public static final ModConfigSpec.ConfigValue<String> lastIP;
    public static final ModConfigSpec.ConfigValue<Boolean> allowPvp;
    public static final ModConfigSpec.ConfigValue<Boolean> requireLicense;
    public static final ModConfigSpec.ConfigValue<Integer> libraryId;
    public static final ModConfigSpec.ConfigValue<Integer> autoLibraryId;
    public static final ModConfigSpec.ConfigValue<Boolean> whitelistMode;
    public static final ModConfigSpec.ConfigValue<ArrayList<String>> friends;
    public static final ModConfigSpec.ConfigValue<Integer> portMapperIndex;
    public static final ModConfigSpec.ConfigValue<Boolean> updateNotifications;
    public static final ModConfigSpec.ConfigValue<Boolean> licenseNotifications;
    public static final ModConfigSpec.ConfigValue<Boolean> whitelistNotifications;
    public static final ModConfigSpec.ConfigValue<Boolean> hideIP;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("Open2Online config");

        port = builder.comment("Server port. Default value is 25565.").define("Port", 25565);
        maxPlayers = builder.comment("Server max players. Default value is 8.").define("Max Players", 8);
        lastIP = builder.comment("Your last IP. Default value is \"0.0.0.0\".").define("Last IP", "0.0.0.0");
        allowPvp = builder.comment("Allow PVP on the server. Default value is true.").define("Allow PVP", true);
        requireLicense = builder.comment("Verify that joining players own the game. Turning this off lets anyone join under any name. Default value is false.").define("Require License", false);
        libraryId = builder.comment("Port opening library. Don't change if everything works fine. -1 - Auto; 0 - WeUPnP; 1 - WaifUPnP; 2 - PortMapper. Default value is -1.").define("Library", -1);
        autoLibraryId = builder.comment("Library that last succeeded in Auto mode, so the next run can start with it. Don't change it.").define("AutoLibrary", -1);
        whitelistMode = builder.comment("Enable whitelist mode. Default value is false.").define("Whitelist", false);
        friends = builder.comment("List of friends who can join the server if the whitelist mode is enabled.").define("FriendList", new ArrayList<>());
        updateNotifications = builder.comment("Chat message about an update.").define("UpdateNotifications", true);
        licenseNotifications = builder.comment("Chat warning when the server is published with the license check off.").define("LicenseNotifications", true);
        whitelistNotifications = builder.comment("Chat warning when the server is published with the whitelist off.").define("WhitelistNotifications", true);
        portMapperIndex = builder.comment("Needed for faster port opening using PortMapper. Don't change it.").define("PortMapperIndex", -1);
        hideIP = builder.comment("Hide your IP.").define("HideIP", true);

        builder.pop();
        SPEC = builder.build();
    }

    private OpenToOnlineConfig() {
    }

    /**
     * Config values backed by NightConfig are not re-saved when the returned collection is mutated
     * in place, so writes to the friend list have to be pushed back explicitly.
     */
    public static void setFriends(ArrayList<String> updated) {
        friends.set(updated);
        friends.save();
    }
}
