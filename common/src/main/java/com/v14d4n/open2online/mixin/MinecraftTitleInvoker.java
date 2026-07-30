package com.v14d4n.open2online.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Opens up {@code Minecraft.updateTitle()}, which is private.
 *
 * <p>{@code createTitle()} only builds the string; the window is repainted by {@code updateTitle()},
 * and vanilla calls that on its own schedule. Nothing triggers it when a world is published, so the
 * marker {@link MixinMinecraft} adds only showed up once some unrelated event refreshed the title.
 */
@Mixin(Minecraft.class)
public interface MinecraftTitleInvoker {
    @Invoker("updateTitle")
    void open2online$updateTitle();
}
