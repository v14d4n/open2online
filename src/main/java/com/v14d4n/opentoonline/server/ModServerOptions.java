package com.v14d4n.opentoonline.server;

import com.mojang.blaze3d.platform.GlStateManager;
import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import com.v14d4n.opentoonline.network.upnp.UPnPLibraries;
import com.v14d4n.opentoonline.screens.EditWhitelistScreen;
import net.minecraft.client.AbstractOption;
import net.minecraft.client.GameSettings;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.GPUWarning;
import net.minecraft.client.settings.*;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.Library;

import java.util.function.BiFunction;

import static com.v14d4n.opentoonline.OpenToOnline.minecraft;

@OnlyIn(Dist.CLIENT)
public abstract class ModServerOptions {

    private static int libraryId;
    private static boolean allowPvp;
    private static boolean whitelistMode;

    private static final ITextComponent LIBRARY_TOOLTIP = new TranslationTextComponent("tooltip.opentoonline.library");
    public static final IteratableOption LIBRARY = new IteratableOption("options.opentoonline.library", (p_216577_0_, p_216577_1_) -> {
        libraryId = UPnPLibraries.getById(libraryId + p_216577_1_).getId();
    }, (p_216633_0_, p_216633_1_) -> {
        p_216633_1_.setTooltip(Minecraft.getInstance().font.split(LIBRARY_TOOLTIP, 200));
        return new TranslationTextComponent("options.opentoonline.library").append(": ").append(UPnPLibraries.getById(libraryId).getTextComponent());
    });

    public static final BooleanOption ALLOW_PVP = new BooleanOption("options.opentoonline.allowPvp",
        (p_225287_0_) -> allowPvp,
        (p_225259_0_, p_225259_1_) -> allowPvp = p_225259_1_
    );

    public static final BooleanOption WHITELIST_MODE = new BooleanOption("gui.opentoonline.whitelistMode",
        (getter) -> OpenToOnlineConfig.whitelistMode.get(),
        (gameSettings, value) -> OpenToOnlineConfig.whitelistMode.set(value)
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

    public static void update() {
        libraryId = OpenToOnlineConfig.libraryId.get();
        allowPvp = OpenToOnlineConfig.allowPvp.get();
    }

    public static void save() {
        OpenToOnlineConfig.libraryId.set(libraryId);
        OpenToOnlineConfig.allowPvp.set(allowPvp);
    }
}
