package com.v14d4n.open2online.mixin;

import com.v14d4n.open2online.network.UPnPHandler;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Marks the window title while the world is exposed to the internet. */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Inject(method = "createTitle", at = @At("HEAD"), cancellable = true)
    private void open2online$markOnlineSession(CallbackInfoReturnable<String> cir) {
        if (!UPnPHandler.getClosePortAfterLogout()) {
            return;
        }

        String multiplayer = Component.translatable("title.open2online.multiplayer").getString();
        cir.setReturnValue("Minecraft* " + SharedConstants.getCurrentVersion().name()
                + " - " + multiplayer + " (Open2Online)");
    }
}
