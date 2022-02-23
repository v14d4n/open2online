package com.v14d4n.opentoonline.server;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum EnumLibraries {
    WaifUPnP(0);

    private final int id;

    EnumLibraries(int id) {
        this.id = id;
    }

    public int getId(){
        return id;
    }

    public Component getTextComponent() {
        return new TextComponent(this.name());
    }

    public static EnumLibraries getById(int id) {
        return EnumLibraries.values()[id];
    }
}
