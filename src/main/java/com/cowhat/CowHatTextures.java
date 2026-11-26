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
 * GL context that does not exist yet during mod init.
 */
public final class CowHatTextures {
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("cowhat-textures.json");
    private static final Path TEXTURE_DIR = FabricLoader.getInstance().getConfigDir().resolve("cowhat/textures");
    private static final PlayerList AUTH = new PlayerList(
            FabricLoader.getInstance().getConfigDir().resolve("cowhat-textures-allowlist.json"),
            "Add Minecraft usernames and/or UUIDs here to let them use custom textures (see cowhat-textures.json for the rules). Independent of cowhat-allowlist.json.",
            "snowrain", "Becausepainfully", "jagling", "Keekraft");
    private static final String COMMENT = "Map a case-insensitive substring of an item's display name to a PNG "
            + "placed in config/cowhat/textures/. A match here is drawn above the head instead of the built-in "
            + "Star of David. Example: {\"match\": \"birthday\", \"file\": \"party_hat.png\"}. "
            + "Can also be edited with /hatrix addtexture <match> <file>.";
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static boolean loaded = false;

    private CowHatTextures() {}

    public static boolean isAuthorized(String name, UUID uuid) {
        return AUTH.contains(name, uuid);
    }

    /** A loaded rule: a lowercase substring to match, the render type for its texture, and the texture's aspect ratio. */
    public static final class Entry {
        private final String match;
        private final RenderType renderType;
        private final float aspect; // width / height

        Entry(String match, RenderType renderType, float aspect) {
            this.match = match;
            this.renderType = renderType;
            this.aspect = aspect;
        }

        public RenderType renderType() { return renderType; }
        public float aspect() { return aspect; }
    }

    private static final class Rule {
        String match;
        String file;
    }

    private static final class Config {
        String _comment;
        Rule[] rules;
    }

    public static synchronized void load() {
        ENTRIES.clear();
        if (!Files.exists(CONFIG)) {
            writeTemplate();
            loaded = true;
            return;
        }
        try {
            String json = Files.readString(CONFIG, StandardCharsets.UTF_8);
            Config cfg = new Gson().fromJson(json, Config.class);
            if (cfg != null && cfg.rules != null) {
                int index = 0;
                for (Rule rule : cfg.rules) {
                    Entry entry = loadEntry(rule, index++);
                    if (entry != null) ENTRIES.add(entry);
                }
            }
        } catch (IOException | RuntimeException e) {
            CowHatClient.LOGGER.warn("Cow Hat: could not read {}, no custom textures loaded", CONFIG, e);
        }
        loaded = true;
    }

    private static Entry loadEntry(Rule rule, int index) {
        if (rule == null || rule.match == null || rule.match.isEmpty() || rule.file == null) return null;
        Path file = TEXTURE_DIR.resolve(rule.file);
        if (!Files.exists(file)) {
            CowHatClient.LOGGER.warn("Cow Hat: texture {} (for match \"{}\") does not exist", file, rule.match);
            return null;
        }
        try {
            NativeImage image = NativeImage.read(Files.readAllBytes(file));
            float aspect = (float) image.getWidth() / image.getHeight();
            Identifier id = Identifier.fromNamespaceAndPath("cowhat", "custom/texture_" + index);
            // Constructor uploads the image to the GPU itself.
            DynamicTexture texture = new DynamicTexture(() -> "Cow Hat custom texture " + index, image);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            RenderType renderType = RenderTypes.entityCutoutCull(id);
            return new Entry(rule.match.toLowerCase(Locale.ROOT), renderType, aspect);
        } catch (IOException e) {
            CowHatClient.LOGGER.warn("Cow Hat: failed to load texture {}", file, e);
            return null;
        }
    }

    private static void writeTemplate() {
        try {
            Files.createDirectories(TEXTURE_DIR);
            Config cfg = new Config();
            cfg._comment = COMMENT;
            cfg.rules = new Rule[0];
            Files.writeString(CONFIG, new GsonBuilder().setPrettyPrinting().create().toJson(cfg), StandardCharsets.UTF_8);
        } catch (IOException e) {
            CowHatClient.LOGGER.warn("Cow Hat: could not write default texture config", e);
        }
    }

    /**
     * Appends a new match rule to {@code cowhat-textures.json} and reloads (re-uploading every
     * texture, since indices may shift). Returns false, without changing anything, if {@code
     * file} doesn't exist under {@code config/cowhat/textures/} yet — the PNG has to be placed
     * there first; this never fetches anything over the network.
     */
    public static synchronized boolean addRule(String match, String file) {
        if (match == null || match.isEmpty() || file == null || file.isEmpty()) return false;
        if (!Files.exists(TEXTURE_DIR.resolve(file))) return false;

        Config cfg = null;
        if (Files.exists(CONFIG)) {
            try {
                cfg = new Gson().fromJson(Files.readString(CONFIG, StandardCharsets.UTF_8), Config.class);
            } catch (IOException | RuntimeException e) {
                CowHatClient.LOGGER.warn("Cow Hat: could not read {} before adding a rule, starting fresh", CONFIG, e);
            }
        }
        if (cfg == null) cfg = new Config();
        Rule[] old = cfg.rules == null ? new Rule[0] : cfg.rules;
        Rule[] updated = new Rule[old.length + 1];
        System.arraycopy(old, 0, updated, 0, old.length);
        Rule rule = new Rule();
        rule.match = match;
        rule.file = file;
        updated[old.length] = rule;
        cfg.rules = updated;
        cfg._comment = COMMENT;

        try {
            Files.createDirectories(CONFIG.getParent());
            Files.writeString(CONFIG, new GsonBuilder().setPrettyPrinting().create().toJson(cfg), StandardCharsets.UTF_8);
        } catch (IOException e) {
            CowHatClient.LOGGER.warn("Cow Hat: could not write {}", CONFIG, e);
            return false;
        }
        loaded = false;
        load(); // re-reads everything, including the new rule, and re-uploads the textures
        return true;
    }

    /**
     * Copies a PNG the player picked (from anywhere on their own disk — this never touches
     * the network) into {@code config/cowhat/textures/}, under a name that won't clobber an
     * existing file, then calls {@link #addRule}. Returns the saved file's name on success, or
     * null if the copy or the rule failed.
     */
    public static synchronized String importImage(Path source, String match) {
        if (source == null || match == null || match.isBlank()) return null;
        try {
            Files.createDirectories(TEXTURE_DIR);
            String name = source.getFileName().toString();
            if (!name.toLowerCase(Locale.ROOT).endsWith(".png")) name = name + ".png";
            String stem = name.substring(0, name.length() - 4);
            Path dest = TEXTURE_DIR.resolve(name);
            int i = 1;
            while (Files.exists(dest)) {
                dest = TEXTURE_DIR.resolve(stem + "_" + i + ".png");
                i++;
            }
            Files.copy(source, dest);
            String savedName = dest.getFileName().toString();
            return addRule(match, savedName) ? savedName : null;
        } catch (IOException e) {
            CowHatClient.LOGGER.warn("Cow Hat: failed to import image {}", source, e);
            return null;
        }
    }

    /** First configured entry whose match text is contained in the item's name, or null. */
    public static Entry find(String itemName) {
        if (!loaded) load();
        if (itemName == null || ENTRIES.isEmpty()) return null;
        String lower = itemName.toLowerCase(Locale.ROOT);
        for (Entry e : ENTRIES) {
            if (lower.contains(e.match)) return e;
        }
        return null;
    }

    /** Draws a flat, camera-forward-facing textured quad the given entry's texture, sized by its aspect ratio. */
    public static void renderQuad(PoseStack.Pose pose, VertexConsumer vc, Entry entry, int light) {
        float h = 0.6F;
        float w = h * entry.aspect();
        float halfW = w / 2F, halfH = h / 2F;
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
