package com.v14d4n.opentoonline.server;

import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.network.upnp.UPnPLibraries;
import com.v14d4n.opentoonline.screens.EditWhitelistScreen;
import net.minecraft.client.AbstractOption;
import net.minecraft.client.GameSettings;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.settings.BooleanOption;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.v14d4n.opentoonline.OpenToOnline.minecraft;

@OnlyIn(Dist.CLIENT)
public abstract class ModServerOptions {

    private static int libraryId;
    private static boolean allowPvp;
    private static boolean whitelistMode;

    public static final CycleOption<UPnPLibraries> LIBRARY = CycleOption.create("options.opentoonline.library",
            UPnPLibraries.values(),
            UPnPLibraries::getTextComponent,
            (getter) ->  {
                libraryId = OpenToOnlineConfig.libraryId.get();
                return UPnPLibraries.getById(libraryId);
            },
            (a, b, library) -> libraryId = library.getId()
    ).setTooltip((pTooltip) -> (p_193636_) -> pTooltip.font.split(new TranslationTextComponent("tooltip.opentoonline.library"), 200));

    public static final BooleanOption ALLOW_PVP = new BooleanOption("options.opentoonline.allowPvp",
            (getter) -> {
                allowPvp = OpenToOnlineConfig.allowPvp.get();
                return allowPvp;
            },
            (pOptions, pOption, pValue) -> allowPvp = pValue
    );

    public static final BooleanOption WHITELIST_MODE = new BooleanOption("gui.opentoonline.whitelistMode",
            (getter) -> {
                whitelistMode = OpenToOnlineConfig.whitelistMode.get();
                return whitelistMode;
            },
            (pOptions, pOption, pValue) -> whitelistMode = pValue
    );

    public static final AbstractOption EDIT_WHITELIST = new AbstractOption("gui.opentoonline.editWhitelist") {

        @Override
        public Widget createButton(GameSettings pOptions, int pX, int pY, int pWidth) {
            return new Button(pX, pY, pWidth, 20, new TranslationTextComponent("gui.opentoonline.editWhitelist"),
                    (onClick) -> {
                        ModServerOptions.save();
                        minecraft.setScreen(new EditWhitelistScreen(minecraft.screen));
                    });
        }
    };

    public static void save() {
        OpenToOnlineConfig.libraryId.set(libraryId);
        OpenToOnlineConfig.allowPvp.set(allowPvp);
        OpenToOnlineConfig.whitelistMode.set(whitelistMode);
    }
}
