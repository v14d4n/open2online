package com.v14d4n.opentoonline.mixin;

import com.v14d4n.opentoonline.OpenToOnline;
import com.v14d4n.opentoonline.network.ServerHandler;
import com.v14d4n.opentoonline.screens.ShareToOnlineScreen;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.LanguageScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IngameMenuScreen.class)
public abstract class MixinPauseScreen extends Screen {

    protected MixinPauseScreen(TextComponent pTitle) {
        super(pTitle);
    }

    @Inject(method = "Lnet/minecraft/client/gui/screen/IngameMenuScreen;createPauseMenu()V", at = @At("TAIL"))
    protected void onInit(CallbackInfo ci) {
        boolean active = !ServerHandler.isServerPublished();
        createOpenToOnlineMenuButton(active);
    }

    private void createOpenToOnlineMenuButton(boolean active) {
        int pXOffset = active ? 0 : 20;
        int pYDiff = active ? 20 : 0;

        this.addButton(new ImageButton(width / 2 + 104, height / 4 + 96 - 16, 20, 20,
                pXOffset, 0, pYDiff,
                new ResourceLocation(OpenToOnline.MOD_ID, "textures/gui/button.png"), 64, 64,
                (p_213090_1_) -> this.getMinecraft().setScreen(new ShareToOnlineScreen(this))
        )).active = active;
    }
}