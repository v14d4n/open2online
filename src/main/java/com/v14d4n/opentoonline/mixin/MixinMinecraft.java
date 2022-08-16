package com.v14d4n.opentoonline.mixin;

import com.v14d4n.opentoonline.network.UPnPHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "createTitle()Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void createTitle(CallbackInfoReturnable<String> cir) {
        if (UPnPHandler.getClosePortAfterLogout()) {
            String title = "Minecraft* " + SharedConstants.getCurrentVersion().getName() + " - Multiplayer (Open2Online)";
            cir.setReturnValue(title);
        }
    }
}
