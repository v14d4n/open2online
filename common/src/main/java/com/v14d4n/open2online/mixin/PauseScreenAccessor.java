package com.v14d4n.open2online.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

@Mixin(PauseScreen.class)
public interface PauseScreenAccessor {
    @Accessor("SHARE_TO_LAN")
    static Component open2online$getShareToLanComponent() {
        throw new AssertionError();
    }

    @Accessor("PLAYER_REPORTING")
    static Component open2online$getPlayerReportingComponent() {
        throw new AssertionError();
    }
}
