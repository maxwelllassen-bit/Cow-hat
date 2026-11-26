package com.cowhat;

import net.fabricmc.loader.api.FabricLoader;

import java.util.UUID;

/**
 * Gates the star swap and custom-texture features to an explicit allowlist of players,
 * checked by the wearer's own account name/UUID only.
 *
 * Edit {@code config/cowhat-allowlist.json} to authorize players, e.g.:
 * {"names": ["Notch"], "uuids": ["069a79f4-44e9-4726-a5be-fca90e38aaf5"]}
 */
public final class CowHatAuth {
    private static final PlayerList LIST = new PlayerList(
            FabricLoader.getInstance().getConfigDir().resolve("cowhat-allowlist.json"),
            "Add Minecraft usernames and/or UUIDs here to give them the Star of David swap (and, if matched, custom textures).",
            "snowrain", "Becausepainfully", "jagling", "Keekraft");

    private CowHatAuth() {}

    public static void load() {
        LIST.load();
    }

    public static boolean isAuthorized(String name, UUID uuid) {
        return LIST.contains(name, uuid);
    }
}
