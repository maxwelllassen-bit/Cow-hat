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
 */
public final class HatrixSettings {
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("hatrix-settings.json");
    private static boolean customTextureOverlay = false;
    private static boolean loaded = false;

    private HatrixSettings() {}

    private static final class Data {
        boolean customTextureOverlay;
    }

    public static synchronized void load() {
        if (Files.exists(CONFIG)) {
            try {
                String json = Files.readString(CONFIG, StandardCharsets.UTF_8);
                Data data = new Gson().fromJson(json, Data.class);
                if (data != null) customTextureOverlay = data.customTextureOverlay;
            } catch (IOException | RuntimeException e) {
                CowHatClient.LOGGER.warn("Cow Hat: could not read {}, using defaults", CONFIG, e);
            }
        }
        loaded = true;
    }

    public static synchronized boolean isCustomTextureOverlay() {
        if (!loaded) load();
        return customTextureOverlay;
    }

    /** Flips the setting, persists it, and returns the new value. */
    public static synchronized boolean toggleCustomTextureOverlay() {
        if (!loaded) load();
        customTextureOverlay = !customTextureOverlay;
        save();
        return customTextureOverlay;
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG.getParent());
            Files.writeString(CONFIG,
                    "{\n  \"customTextureOverlay\": " + customTextureOverlay + "\n}\n",
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            CowHatClient.LOGGER.warn("Cow Hat: could not write {}", CONFIG, e);
        }
    }
}
