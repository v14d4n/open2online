package com.v14d4n.open2online.neoforge;

import com.v14d4n.open2online.OpenToOnline;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * {@code dist} says what the Fabric side has always said through {@code "environment": "client"}.
 *
 * <p>Without it NeoForge constructs this on a dedicated server too. The {@code side = "CLIENT"} lines
 * in {@code neoforge.mods.toml} do not say otherwise: those sit in the dependency blocks and are
 * about where neoforge, minecraft and architectury are needed, not about where this mod belongs.
 */
@Mod(value = OpenToOnline.MOD_ID, dist = Dist.CLIENT)
public final class OpenToOnlineNeoForge {
    public OpenToOnlineNeoForge() {
        OpenToOnline.init();
    }
}
