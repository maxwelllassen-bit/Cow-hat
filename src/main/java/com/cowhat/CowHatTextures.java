package com.cowhat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Lets players map a custom texture onto the head overlay, keyed by (a substring of) the
 * worn head item's display name, overriding the built-in Star of David for that item.
 *
 * This is gated by its own allowlist ({@code cowhat-textures-allowlist.json}), independent of
 * {@link CowHatAuth} (the star's list) — a wearer doesn't need to be on the star's allowlist
 * to get a custom texture, and vice versa. It also doesn't require a "cow"-named item; a rule
 * can match any item name.
 *
 * Config: {@code config/cowhat-textures.json} (match rules). PNGs referenced by it live in
 * {@code config/cowhat/textures/}. Loading (including GPU texture upload) happens lazily on
 * the render thread, the first time any entity's head is examined, since it needs an active
 * GPU context that does not exist yet during mod init.
 */
public final class CowHatTextures {
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("cowhat-textures.json");
    private static final Path TEXTURE_DIR = FabricLoader.getInstance().getConfigDir().resolve("cowhat/textures").toAbsolutePath().normalize();
    private static final PlayerList AUTH = new PlayerList(
            FabricLoader.getInstance().getConfigDir().resolve("cowhat-textures-allowlist.json"),
            "Add Minecraft usernames and/or UUIDs here to let them use custom textures (see cowhat-textures.json for the rules). Independent of cowhat-allowlist.json.",
            "snowrain", "Becausepainfully", "jagling", "Keekraft");
    private static final String COMMENT = "Map a case-insensitive substring of an item's display name to a PNG "
            + "placed in config/cowhat/textures/. A match here is drawn above the head instead of the built-in "
            + "Star of David. Example: {\"match\": \"birthday\", \"file\": \"party_hat.png\"}. "
            + "Can also be edited with /hatrix addtexture <match> <file>.";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Immutable snapshot, swapped whole on reload; null until first loaded. */
    private static volatile List<Entry> entries;
    /** Texture ids registered by the last load, released on the next one so reloads don't leak GPU memory. */
    private static final List<Identifier> registered = new ArrayList<>();

    private CowHatTextures() {}

    public static void loadAllowlist() {
        AUTH.load();
    }

    /** {@code lowerName} must already be lowercased (see {@link PlayerList#contains}). */
    public static boolean isAuthorized(String lowerName, UUID uuid) {
        return AUTH.contains(lowerName, uuid);
    }

    /** A loaded rule: a lowercase substring to match, the render type for its texture, and the texture's aspect ratio. */
    public record Entry(String match, RenderType renderType, float aspect) {}

    private static final class Rule {
        String match;
        String file;
    }

    private static final class Config {
        String _comment;
        Rule[] rules;
    }

    public static synchronized void load() {
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        for (Identifier id : registered) textures.release(id);
        registered.clear();

        List<Entry> loaded = new ArrayList<>();
        if (!Files.exists(CONFIG)) {
            writeConfig(new Rule[0]);
        } else {
            Config cfg = readConfig();
            if (cfg != null && cfg.rules != null) {
                for (Rule rule : cfg.rules) {
                    Entry entry = loadEntry(textures, rule, registered.size());
                    if (entry != null) loaded.add(entry);
                }
            }
        }
        entries = List.copyOf(loaded);
    }

    private static Config readConfig() {
        try {
            return GSON.fromJson(Files.readString(CONFIG, StandardCharsets.UTF_8), Config.class);
        } catch (IOException | RuntimeException e) {
            CowHatClient.LOGGER.warn("Hatrix: could not read {}", CONFIG, e);
            return null;
        }
    }

    /** Resolves a rule's file name inside the texture folder, or null if it would escape it. */
    private static Path resolveTexture(String file) {
        if (file == null || file.isBlank()) return null;
        Path p = TEXTURE_DIR.resolve(file).normalize();
        return p.startsWith(TEXTURE_DIR) ? p : null;
    }

    private static Entry loadEntry(TextureManager textures, Rule rule, int index) {
        if (rule == null || rule.match == null || rule.match.isEmpty()) return null;
        Path file = resolveTexture(rule.file);
        if (file == null || !Files.isRegularFile(file)) {
            CowHatClient.LOGGER.warn("Hatrix: texture \"{}\" (for match \"{}\") is not a file in {}", rule.file, rule.match, TEXTURE_DIR);
            return null;
        }
        NativeImage image = null;
        try {
            image = NativeImage.read(Files.readAllBytes(file));
            float aspect = (float) image.getWidth() / image.getHeight();
            Identifier id = Identifier.fromNamespaceAndPath("cowhat", "custom/texture_" + index);
            // The DynamicTexture takes ownership of the image and uploads it itself.
            DynamicTexture texture = new DynamicTexture(() -> "Hatrix custom texture " + index, image);
            image = null;
            textures.register(id, texture);
            registered.add(id);
            // Not back-face culled: the quad is flat, so it must stay visible from behind the wearer.
            return new Entry(rule.match.toLowerCase(Locale.ROOT), RenderTypes.entityCutout(id), aspect);
        } catch (IOException | RuntimeException e) {
            CowHatClient.LOGGER.warn("Hatrix: failed to load texture {}", file, e);
            if (image != null) image.close();
            return null;
        }
    }

    private static boolean writeConfig(Rule[] rules) {
        Config cfg = new Config();
        cfg._comment = COMMENT;
        cfg.rules = rules;
        try {
            Files.createDirectories(TEXTURE_DIR);
            Files.writeString(CONFIG, GSON.toJson(cfg), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            CowHatClient.LOGGER.warn("Hatrix: could not write {}", CONFIG, e);
            return false;
        }
    }

    /**
     * Appends a new match rule to {@code cowhat-textures.json} and reloads (re-uploading every
     * texture, since indices may shift). The PNG must already be in {@code config/cowhat/textures/};
     * this never fetches anything over the network. Returns null on success, or a short reason
     * for the failure, without changing anything.
     */
    public static synchronized String addRule(String match, String file) {
        if (match == null || match.isBlank()) return "the match text is empty";
        Path path = resolveTexture(file);
        if (path == null) return "\"" + file + "\" is not a file name inside config/cowhat/textures/";
        if (!Files.isRegularFile(path)) return "config/cowhat/textures/" + file + " doesn't exist — place the PNG there first";
        try (NativeImage ignored = NativeImage.read(Files.readAllBytes(path))) {
            // Validated only; the real upload happens in load().
        } catch (IOException | RuntimeException e) {
            return file + " isn't a readable PNG";
        }

        Rule[] old = new Rule[0];
        if (Files.exists(CONFIG)) {
            Config cfg = readConfig();
            if (cfg == null) return "cowhat-textures.json couldn't be read — fix or delete it first";
            if (cfg.rules != null) old = cfg.rules;
        }
        Rule rule = new Rule();
        rule.match = match;
        rule.file = file;
        Rule[] updated = java.util.Arrays.copyOf(old, old.length + 1);
        updated[old.length] = rule;
        if (!writeConfig(updated)) return "couldn't write cowhat-textures.json (see log)";
        load();
        return null;
    }

    /**
     * Copies a PNG the player picked (from anywhere on their own disk — this never touches
     * the network) into {@code config/cowhat/textures/}, under a name that won't clobber an
     * existing file, then calls {@link #addRule}. Returns null on success, or a short reason.
     */
    public static synchronized String importImage(Path source, String match) {
        if (source == null || match == null || match.isBlank()) return "nothing to import";
        Path dest;
        try {
            Files.createDirectories(TEXTURE_DIR);
            String name = source.getFileName().toString();
            if (!name.toLowerCase(Locale.ROOT).endsWith(".png")) name = name + ".png";
            String stem = name.substring(0, name.length() - 4);
            dest = TEXTURE_DIR.resolve(name);
            for (int i = 1; Files.exists(dest); i++) dest = TEXTURE_DIR.resolve(stem + "_" + i + ".png");
            Files.copy(source, dest);
        } catch (IOException e) {
            CowHatClient.LOGGER.warn("Hatrix: failed to import image {}", source, e);
            return "couldn't copy the file (see log)";
        }
        String error = addRule(match, dest.getFileName().toString());
        if (error != null) {
            try {
                Files.deleteIfExists(dest);
            } catch (IOException ignored) {
                // Leaving an unused copy behind is harmless.
            }
        }
        return error;
    }

    /** First configured entry whose match text is contained in the (already lowercased) item name, or null. */
    public static Entry find(String lowerItemName) {
        if (lowerItemName == null) return null;
        List<Entry> list = entries;
        if (list == null) {
            load();
            list = entries;
        }
        for (Entry e : list) {
            if (lowerItemName.contains(e.match())) return e;
        }
        return null;
    }

    /** Draws a flat, head-facing textured quad for the given entry, sized by its aspect ratio. */
    public static void renderQuad(PoseStack.Pose pose, VertexConsumer vc, Entry entry, int light) {
        float halfH = 0.3F;
        float halfW = halfH * entry.aspect();
        quadVertex(pose, vc, -halfW, -halfH, 0F, 0F, light);
        quadVertex(pose, vc, -halfW, halfH, 0F, 1F, light);
        quadVertex(pose, vc, halfW, halfH, 1F, 1F, light);
        quadVertex(pose, vc, halfW, -halfH, 1F, 0F, light);
    }

    private static void quadVertex(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float u, float v, int light) {
        vc.addVertex(pose, x, y, 0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0F, 0F, 1F);
    }
}
