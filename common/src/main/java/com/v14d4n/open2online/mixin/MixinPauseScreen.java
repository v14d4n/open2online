package com.v14d4n.open2online.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.v14d4n.open2online.OpenToOnline;
import com.v14d4n.open2online.network.ServerHandler;
import com.v14d4n.open2online.screens.ShareToOnlineScreen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

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

        Button anchorButton = this.children().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> button.getMessage().equals(PauseScreenAccessor.open2online$getShareToLanComponent())
                        || button.getMessage().equals(PauseScreenAccessor.open2online$getPlayerReportingComponent()))
                .findFirst()
                .orElse(null);

        int x = this.width / 2 + 104; // bruh
        int y = this.height / 4 + 96 - 16;

        if (anchorButton != null) {
            x = anchorButton.getX() + anchorButton.getWidth() + 2;
            y = anchorButton.getY();
        }

        ImageButton button = new ImageButton(
                x, y, 20, 20,
                OPEN_TO_ONLINE_SPRITES,
                press -> this.minecraft.setScreen(new ShareToOnlineScreen(this)),
                Component.translatable("gui.open2online.onlineWorld"));

        button.active = active;
        this.addRenderableWidget(button);
    }
}
