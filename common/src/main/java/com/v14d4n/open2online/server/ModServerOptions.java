package com.v14d4n.open2online.server;

import com.v14d4n.open2online.config.OpenToOnlineConfig;
import com.v14d4n.open2online.network.nat.UPnPLibraries;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;


import java.util.List;

/**
 * The 1.16.5 build kept every value in static mutable fields and pushed them into the config from an
 * explicit {@code save()}. {@code OptionInstance} carries its own change callback, so each option
 * writes straight through to the config instead.
 */
@Environment(EnvType.CLIENT)
public final class ModServerOptions {
    private ModServerOptions() {
    }

    public static OptionInstance<UPnPLibraries> library() {
        return new OptionInstance<>(
                "options.open2online.library",
                OptionInstance.cachedConstantTooltip(Component.translatable("tooltip.open2online.library")),
                // Only the value: CycleButton prepends "caption: " itself, the same way vanilla's
                // BOOLEAN_TO_STRING returns a bare ON/OFF.
                (caption, value) -> value.caption(),
                new OptionInstance.Enum<>(List.of(UPnPLibraries.values()), UPnPLibraries.CODEC),
                UPnPLibraries.getById(OpenToOnlineConfig.libraryId.get()),
                value -> {
                    OpenToOnlineConfig.libraryId.set(value.getId());
                    OpenToOnlineConfig.libraryId.save();
                });
    }

    public static OptionInstance<Boolean> allowPvp() {
        return booleanOption("options.open2online.allowPvp", OpenToOnlineConfig.allowPvp);
    }

    /** Carries a tooltip because switching it off has a security consequence worth spelling out. */
    public static OptionInstance<Boolean> requireLicense() {
        return OptionInstance.createBoolean("options.open2online.licenseRequired",
                OptionInstance.cachedConstantTooltip(Component.translatable("tooltip.open2online.licenseRequired")),
                OpenToOnlineConfig.requireLicense.get(),
                updated -> {
                    OpenToOnlineConfig.requireLicense.set(updated);
                    OpenToOnlineConfig.requireLicense.save();
                });
    }

    public static OptionInstance<Boolean> updateNotifications() {
        return booleanOption("options.open2online.notify.update", OpenToOnlineConfig.updateNotifications);
    }

    public static OptionInstance<Boolean> licenseNotifications() {
        return booleanOption("options.open2online.notify.license", OpenToOnlineConfig.licenseNotifications);
    }

    public static OptionInstance<Boolean> whitelistNotifications() {
        return booleanOption("options.open2online.notify.whitelist", OpenToOnlineConfig.whitelistNotifications);
    }

    public static OptionInstance<Boolean> autoStart() {
        return booleanOption("options.open2online.autoStart", OpenToOnlineConfig.autoStart);
    }

    /** Slider rather than a cycle button: a range of seconds is what vanilla shows as a slider. */
    public static OptionInstance<Integer> autoStartDelay() {
        return new OptionInstance<>(
                "options.open2online.autoStart.delay",
                OptionInstance.cachedConstantTooltip(
                        Component.translatable("tooltip.open2online.autoStart.delay")),
                (caption, value) -> Component.translatable("options.open2online.autoStart.delay.value", value),
                new OptionInstance.IntRange(OpenToOnlineConfig.AUTO_START_MIN_DELAY,
                        OpenToOnlineConfig.AUTO_START_MAX_DELAY),
                OpenToOnlineConfig.autoStartDelay.get(),
                value -> {
                    OpenToOnlineConfig.autoStartDelay.set(value);
                    OpenToOnlineConfig.autoStartDelay.save();
                });
    }

    /** A boolean underneath, but shown as the two destinations rather than as on/off. */
    public static OptionInstance<Boolean> autoStartMode() {
        return new OptionInstance<>(
                "options.open2online.autoStart.mode",
                OptionInstance.noTooltip(),
                (caption, value) -> Component.translatable(value
                        ? "options.open2online.autoStart.mode.online"
                        : "options.open2online.autoStart.mode.lan"),
                OptionInstance.BOOLEAN_VALUES,
                OpenToOnlineConfig.autoStartOnline.get(),
                value -> {
                    OpenToOnlineConfig.autoStartOnline.set(value);
                    OpenToOnlineConfig.autoStartOnline.save();
                });
    }

    public static OptionInstance<Boolean> whitelistMode() {
        return booleanOption("gui.open2online.whitelistMode", OpenToOnlineConfig.whitelistMode);
    }

    public static OptionInstance<Boolean> hideIP() {
        return booleanOption("gui.open2online.hideIP", OpenToOnlineConfig.hideIP);
    }

    private static OptionInstance<Boolean> booleanOption(String key,
                                                         net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<Boolean> value) {
        return OptionInstance.createBoolean(key, value.get(), updated -> {
            value.set(updated);
            value.save();
        });
    }
}
