package com.v14d4n.opentoonline.network.upnp;

import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum UPnPLibraries {
    WaifUPnP(0, WaifUPnPLibrary::new),
    Weupnp(1, WeupnpLibrary::new);

    private final int id;
    private final Supplier<IUPnPLibrary> librarySupplier;

    UPnPLibraries(int id, Supplier<IUPnPLibrary> librarySupplier) {
        this.id = id;
        this.librarySupplier = librarySupplier;
    }

    public int getId(){
        return id;
    }

    public IUPnPLibrary getHandler() {
        return librarySupplier.get();
    }

    public Component getTextComponent() {
        return new TextComponent(this.name());
    }

    public static UPnPLibraries getById(int id) {
        for (UPnPLibraries library : UPnPLibraries.values())
            if (library.getId() == id)
                return library;

        OpenToOnlineConfig.libraryId.set(0);
        return getById(0);
    }
}
