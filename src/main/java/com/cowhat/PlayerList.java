package com.cowhat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * A JSON-backed list of players, identified only by their own account name and/or UUID —
 * never a credential, token or session value. Used to lock a cosmetic feature to specific
 * players. An empty, missing or unreadable config file means the list is empty (fails closed).
 *
 * Lookups run for every rendered player every frame, so the loaded list is an immutable
 * snapshot (names lowercased, UUIDs parsed once) published through a volatile field: a reader
 * never sees a half-loaded list and never allocates.
 */
public final class PlayerList {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private record Snapshot(Set<String> names, Set<UUID> uuids) {}

    private final Path config;
    private final String comment;
    private final String[] defaultNames;
    private volatile Snapshot snapshot; // null until loaded

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
        if (!Files.exists(config)) writeTemplate();
        Set<String> names = new HashSet<>();
        Set<UUID> uuids = new HashSet<>();
        if (Files.exists(config)) {
            try (Reader r = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
                Data data = GSON.fromJson(r, Data.class);
                if (data != null && data.names != null) {
                    for (String n : data.names) {
                        if (n != null && !n.isBlank()) names.add(n.strip().toLowerCase(Locale.ROOT));
                    }
                }
                if (data != null && data.uuids != null) {
                    for (String u : data.uuids) {
                        if (u == null || u.isBlank()) continue;
                        try {
                            uuids.add(UUID.fromString(u.strip()));
                        } catch (IllegalArgumentException e) {
                            CowHatClient.LOGGER.warn("Hatrix: ignoring invalid UUID \"{}\" in {}", u, config);
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                CowHatClient.LOGGER.warn("Hatrix: could not read {}, list is empty", config, e);
                names.clear();
                uuids.clear();
            }
        }
        snapshot = new Snapshot(Set.copyOf(names), Set.copyOf(uuids));
    }

    private void writeTemplate() {
        Data data = new Data();
        data._comment = comment;
        data.names = defaultNames;
        data.uuids = new String[0];
        try {
            Files.createDirectories(config.getParent());
            Files.writeString(config, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            CowHatClient.LOGGER.warn("Hatrix: could not write default list {}", config, e);
        }
    }

    /**
     * True if the given player (by name or UUID, never by token) is on this list.
     * {@code lowerName} must already be lowercased with {@link Locale#ROOT}; callers lowercase
     * once per player instead of once per list.
     */
    public boolean contains(String lowerName, UUID uuid) {
        Snapshot s = snapshot;
        if (s == null) {
            load();
            s = snapshot;
        }
        return (lowerName != null && s.names.contains(lowerName)) || (uuid != null && s.uuids.contains(uuid));
    }
}
