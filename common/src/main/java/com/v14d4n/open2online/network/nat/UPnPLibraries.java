package com.v14d4n.open2online.network.nat;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Shaped after vanilla's option enums (see {@code ChatVisiblity}): a numeric id, a caption built
 * from a translation key, and a codec for the option widget. {@code OptionEnum} is not used — it is
 * an empty interface in 1.21.11 and vanilla no longer implements it.
 *
 * <p>{@link #AUTO} is a selection mode rather than a backend: it makes the handler try the real
 * ones in declaration order. Ids are the values already stored in existing configs, so they do not
 * follow the declaration order.
 */
public enum UPnPLibraries implements StringRepresentable {
    AUTO(-1, "auto", null),
    WAIFUPNP(1, "waifupnp", WaifUPnPLibrary::new),
    WEUPNP(0, "weupnp", WeUPnPLibrary::new),
    PORTMAPPER(2, "portmapper", PortMapperLibrary::new);

    public static final StringRepresentable.EnumCodec<UPnPLibraries> CODEC =
            StringRepresentable.fromEnum(UPnPLibraries::values);

    /** The order {@link #AUTO} walks through: cheapest and most widely supported first. */
    public static final List<UPnPLibraries> AUTO_ORDER =
            List.of(WAIFUPNP, WEUPNP, PORTMAPPER);

    private final int id;
    private final String serializedName;
    private final Component caption;
    private final Supplier<IUPnPLibrary> librarySupplier;

    UPnPLibraries(int id, String serializedName, Supplier<IUPnPLibrary> librarySupplier) {
        this.id = id;
        this.serializedName = serializedName;
        this.caption = Component.translatable("options.open2online.library." + serializedName);
        this.librarySupplier = librarySupplier;
    }

    public int getId() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public Component caption() {
        return caption;
    }

    public boolean isAuto() {
        return this == AUTO;
    }

    public IUPnPLibrary getHandler() {
        if (librarySupplier == null) {
            throw new IllegalStateException(this + " is a selection mode, not a port mapping backend");
        }
        return librarySupplier.get();
    }

    /**
     * The library a stored id stands for, {@link #AUTO} for anything this version does not know.
     *
     * <p>This is the only check the id gets: the config stores it as a plain number precisely so
     * that the answer to "which numbers are real" lives here, beside the ids themselves. An id from
     * a newer build, or from a hand-edited file, therefore reads as Auto rather than as nothing.
     *
     * <p>It used to write Auto back to the config from here, which was both a surprising thing for a
     * lookup to do — the settings screen calls it while building itself, on the render thread — and
     * unreachable, since the config clamped the id into the range where every value was a real one.
     */
    public static UPnPLibraries getById(int id) {
        for (UPnPLibraries library : values()) {
            if (library.getId() == id) {
                return library;
            }
        }

        return AUTO;
    }

    /**
     * Resolves a remembered backend, ignoring anything that is not a usable one — a stale id or
     * {@link #AUTO} itself both mean "nothing remembered".
     */
    public static Optional<UPnPLibraries> backendById(int id) {
        return AUTO_ORDER.stream().filter(library -> library.getId() == id).findFirst();
    }
}
