package com.v14d4n.open2online.mixin;

import com.v14d4n.open2online.OpenToOnline;
import com.v14d4n.open2online.network.ServerHandler;
import com.v14d4n.open2online.screens.ShareToOnlineScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class MixinPauseScreen extends Screen {
    /**
     * 1.16.5 addressed one 64x64 sheet by pixel offsets; the widget now resolves each state through
     * the GUI sprite atlas, so the sheet was split into these three sprites.
     */
    private static final WidgetSprites OPEN_TO_ONLINE_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(OpenToOnline.MOD_ID, "button"),
            Identifier.fromNamespaceAndPath(OpenToOnline.MOD_ID, "button_disabled"),
            Identifier.fromNamespaceAndPath(OpenToOnline.MOD_ID, "button_focused"),
            Identifier.fromNamespaceAndPath(OpenToOnline.MOD_ID, "button_disabled"));

    protected MixinPauseScreen(Component title) {
        super(title);
    }

    @Inject(method = "createPauseMenu", at = @At("TAIL"))
    private void open2online$addShareButton(CallbackInfo ci) {
        boolean active = !ServerHandler.isServerPublished();

        ImageButton button = new ImageButton(
                this.width / 2 + 104, this.height / 4 + 96 - 16, 20, 20,
                OPEN_TO_ONLINE_SPRITES,
                press -> this.minecraft.setScreen(new ShareToOnlineScreen(this)),
                Component.translatable("gui.open2online.onlineWorld"));

        button.active = active;
        this.addRenderableWidget(button);
    }
}
