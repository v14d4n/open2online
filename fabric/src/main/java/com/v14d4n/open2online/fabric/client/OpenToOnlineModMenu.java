package com.v14d4n.open2online.fabric.client;

import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.UpdateChannel;
import com.terraformersmc.modmenu.api.UpdateChecker;
import com.terraformersmc.modmenu.api.UpdateInfo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Flags the mod in Mod Menu's list when a newer build exists, the same way Mod Menu flags everything
 * else.
 *
 * <p>Mod Menu's own check matches the jar's hash against Modrinth, which only ever recognises jars
 * downloaded from there. Answering out of the list the chat notice reads keeps one source of truth
 * and covers the copies that came from anywhere else.
 *
 * <p>The config screen is deliberately left alone: Architectury already hands ours to Mod Menu, and
 * Mod Menu ignores the unset factory this class inherits rather than letting it overwrite that.
 */
@Environment(EnvType.CLIENT)
public final class OpenToOnlineModMenu implements ModMenuApi {
    @Override
    public UpdateChecker getUpdateChecker() {
        return OpenToOnlineModMenu::lookUp;
    }

    /**
     * Runs on a Mod Menu worker, which is why it is allowed to sit on the network. Ends in a bare
     * {@code null} because that is what the interface asks for — "no update" has no other spelling
     * here.
     */
    private static UpdateInfo lookUp() {
        // Deliberately not tied to the mod's own update notification setting: that one governs the
        // chat line, while this badge is Mod Menu's, switched on and off in Mod Menu's own settings.
        return com.v14d4n.open2online.UpdateChecker.lookUpLatestVersion()
                .filter(latest -> !latest.equals(com.v14d4n.open2online.UpdateChecker.installedVersion()))
                .<UpdateInfo>map(latest -> new Outdated(com.v14d4n.open2online.UpdateChecker.downloadPage()))
                .orElse(null);
    }

    /** No update message of its own, so Mod Menu writes the one it uses for every other mod. */
    private record Outdated(String downloadLink) implements UpdateInfo {
        @Override
        public boolean isUpdateAvailable() {
            return true;
        }

        @Override
        public String getDownloadLink() {
            return downloadLink;
        }

        /** The list only ever names finished releases. */
        @Override
        public UpdateChannel getUpdateChannel() {
            return UpdateChannel.RELEASE;
        }
    }
}
