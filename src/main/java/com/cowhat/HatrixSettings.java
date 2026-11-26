package com.cowhat;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Small persisted client-side settings, toggled via {@code /hatrix}. Currently just whether a
 * matched custom texture replaces the head item (the default, same as before this setting
 * existed) or sits above it additively, the same way cat ears do.
 *
 * Loaded once at client init; the flag is read every frame per rendered player, so the read
 * path is a plain volatile read with no locking.
 */
public final class HatrixSettings {
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("hatrix-settings.json");
    private static volatile boolean customTextureOverlay = false;

    private HatrixSettings() {}

    private static final class Data {
        boolean customTextureOverlay;
    }

    public static synchronized void load() {
        if (!Files.exists(CONFIG)) return;
        try {
            Data data = new Gson().fromJson(Files.readString(CONFIG, StandardCharsets.UTF_8), Data.class);
            if (data != null) customTextureOverlay = data.customTextureOverlay;
        } catch (IOException | RuntimeException e) {
            CowHatClient.LOGGER.warn("Hatrix: could not read {}, using defaults", CONFIG, e);
        }
    }

    public static boolean isCustomTextureOverlay() {
        return customTextureOverlay;
    }

    /** Flips the setting, persists it, and returns the new value. */
    public static synchronized boolean toggleCustomTextureOverlay() {
        boolean value = !customTextureOverlay;
        customTextureOverlay = value;
        try {
            Files.createDirectories(CONFIG.getParent());
            Files.writeString(CONFIG, "{\n  \"customTextureOverlay\": " + value + "\n}\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            CowHatClient.LOGGER.warn("Hatrix: could not write {}", CONFIG, e);
        }
        return value;
    }
}
