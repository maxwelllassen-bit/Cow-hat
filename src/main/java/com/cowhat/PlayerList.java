package com.cowhat;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * A JSON-backed list of players, identified only by their own account name and/or UUID —
 * never a credential, token or session value. Used to lock a cosmetic feature to specific
 * players. An empty or missing config file means the list is empty (fails closed).
 */
public final class PlayerList {
    private final Path config;
    private final String comment;
    private final String[] defaultNames;
    private Set<String> names = Collections.emptySet();
    private Set<String> uuids = Collections.emptySet();
    private boolean loaded = false;

    public PlayerList(Path config, String comment, String... defaultNames) {
        this.config = config;
        this.comment = comment;
        this.defaultNames = defaultNames;
    }

    private static final class Data {
        String _comment;
        String[] names;
        String[] uuids;
    }

    public synchronized void load() {
        names = new HashSet<>();
        uuids = new HashSet<>();
        if (Files.exists(config)) {
            try (Reader r = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
                Data data = new Gson().fromJson(r, Data.class);
                if (data != null) {
                    if (data.names != null) for (String n : data.names) names.add(n.toLowerCase(Locale.ROOT));
                    if (data.uuids != null) for (String u : data.uuids) uuids.add(u.toLowerCase(Locale.ROOT));
                }
            } catch (IOException | RuntimeException e) {
                CowHatClient.LOGGER.warn("Cow Hat: could not read {}, list is empty", config, e);
            }
        } else {
            writeTemplate();
        }
        loaded = true;
    }

    private void writeTemplate() {
        try {
            Files.createDirectories(config.getParent());
            StringBuilder namesJson = new StringBuilder();
            for (int i = 0; i < defaultNames.length; i++) {
                if (i > 0) namesJson.append(", ");
                namesJson.append('"').append(defaultNames[i]).append('"');
            }
            Files.writeString(config,
                    "{\n  \"_comment\": \"" + comment.replace("\"", "\\\"") + "\",\n"
                    + "  \"names\": [" + namesJson + "],\n"
                    + "  \"uuids\": []\n}\n",
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            CowHatClient.LOGGER.warn("Cow Hat: could not write default list {}", config, e);
        }
    }

    /** True if the given player (by name or UUID, never by token) is on this list. */
    public boolean contains(String name, UUID uuid) {
        if (!loaded) load();
        if (names.isEmpty() && uuids.isEmpty()) return false;
        if (name != null && names.contains(name.toLowerCase(Locale.ROOT))) return true;
        return uuid != null && uuids.contains(uuid.toString().toLowerCase(Locale.ROOT));
    }
}
