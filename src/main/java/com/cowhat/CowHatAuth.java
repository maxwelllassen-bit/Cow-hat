package com.cowhat;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Gates the mod's features to an explicit allowlist of players, checked against the
 * locally logged-in account's name/UUID only. No credential, token or session data is
 * read or sent anywhere — this never touches {@code User#getAccessToken()}.
 *
 * Edit {@code config/cowhat-allowlist.json} to authorize players, e.g.:
 * {"names": ["Notch"], "uuids": ["069a79f4-44e9-4726-a5be-fca90e38aaf5"]}
 * An empty/missing file means nobody is authorized (fail closed).
 */
public final class CowHatAuth {
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("cowhat-allowlist.json");
    private static Set<String> names = Collections.emptySet();
    private static Set<String> uuids = Collections.emptySet();
    private static boolean loaded = false;

    private CowHatAuth() {}

    private static final class AllowList {
        String[] names;
        String[] uuids;
    }

    public static synchronized void load() {
        names = new HashSet<>();
        uuids = new HashSet<>();
        if (Files.exists(CONFIG)) {
            try (Reader r = Files.newBufferedReader(CONFIG, StandardCharsets.UTF_8)) {
                AllowList list = new Gson().fromJson(r, AllowList.class);
                if (list != null) {
                    if (list.names != null) for (String n : list.names) names.add(n.toLowerCase(Locale.ROOT));
                    if (list.uuids != null) for (String u : list.uuids) uuids.add(u.toLowerCase(Locale.ROOT));
                }
            } catch (IOException | RuntimeException e) {
                CowHatClient.LOGGER.warn("Cow Hat: could not read {}, nobody will be authorized", CONFIG, e);
            }
        } else {
            writeTemplate();
        }
        loaded = true;
    }

    private static void writeTemplate() {
        try {
            Files.createDirectories(CONFIG.getParent());
            Files.writeString(CONFIG,
                    "{\n  \"_comment\": \"Add Minecraft usernames and/or UUIDs to authorize them for Cow Hat.\",\n"
                    + "  \"names\": [\"snowrain\"],\n"
                    + "  \"uuids\": [\"e74490ab-3bd0-4e3b-8486-233f1bf18ae5\"]\n}\n",
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            CowHatClient.LOGGER.warn("Cow Hat: could not write default allowlist", e);
        }
    }

    /** True if the locally logged-in player (by name or UUID, never by token) is authorized. */
    public static boolean isLocalPlayerAuthorized(String name, java.util.UUID uuid) {
        if (!loaded) load();
        if (names.isEmpty() && uuids.isEmpty()) return false;
        if (name != null && names.contains(name.toLowerCase(Locale.ROOT))) return true;
        return uuid != null && uuids.contains(uuid.toString().toLowerCase(Locale.ROOT));
    }
}
