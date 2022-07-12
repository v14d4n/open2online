package com.v14d4n.opentoonline.network.upnp;

import com.v14d4n.opentoonline.config.OpenToOnlineConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.OptionEnum;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum UPnPLibraries implements OptionEnum {
    WeUPnP(0, WeUPnPLibrary::new),
    WaifUPnP(1, WaifUPnPLibrary::new);

    private final int id;
    private final Supplier<IUPnPLibrary> librarySupplier;

    UPnPLibraries(int id, Supplier<IUPnPLibrary> librarySupplier) {
        this.id = id;
        this.librarySupplier = librarySupplier;
    }

    public int getId() {
        return this.id;
    }

    @Override
    public String getKey() {
        return this.librarySupplier.toString();
    }

    @Override
    public Component getCaption() {
        return OptionEnum.super.getCaption();
    }

    public IUPnPLibrary getHandler() {
        return librarySupplier.get();
    }

    private static final UPnPLibraries[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(UPnPLibraries::getId)).toArray(UPnPLibraries[]::new);

    public static UPnPLibraries byId(int pId) {
        return BY_ID[Mth.positiveModulo(pId, BY_ID.length)];
    }

    public Component getTextComponent() {
        return Component.literal(this.name());
    }

    public static UPnPLibraries getById(int id) {
        for (UPnPLibraries library : UPnPLibraries.values())
            if (library.getId() == id)
                return library;

        OpenToOnlineConfig.libraryId.set(0);
        return getById(0);
    }
}
