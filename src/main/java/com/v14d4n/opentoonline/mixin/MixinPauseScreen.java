package com.v14d4n.opentoonline.mixin;

import com.v14d4n.opentoonline.OpenToOnline;
import com.v14d4n.opentoonline.network.ServerHandler;
import com.v14d4n.opentoonline.screens.ShareToOnlineScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Mixin(PauseScreen.class)
public abstract class MixinPauseScreen extends Screen {

    protected MixinPauseScreen(Component pTitle) {
        super(pTitle);
    }

    @Inject(method = "createPauseMenu()V", at = @At("TAIL"))
    protected void onInit(CallbackInfo ci) {
        boolean active = !ServerHandler.isServerPublished();
        createOpenToOnlineMenuButton(active);
    }

    private void createOpenToOnlineMenuButton(boolean active) {
        int pXOffset = active ? 0 : 20;
        int pYDiff = active ? 20 : 0;

        this.addRenderableWidget(new ImageButton(
                width / 2 + 104, height / 4 + 96 - 16, 20, 20, pXOffset, 0, pYDiff,
                new ResourceLocation(OpenToOnline.MOD_ID, "textures/gui/button.png"), 40,
                40, (p_213088_1_) -> this.getMinecraft().setScreen(new ShareToOnlineScreen(this))
        )).active = active;
    }
}