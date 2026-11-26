package com.cowhat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.loader.api.FabricLoader;

import java.util.UUID;

/**
 * Gives an allowlisted player a bear icon above their head, but only while they're also
 * wearing a "cow"-named head item (unlike cat ears, this is conditional on the cow hat, not
 * independent of it). It's drawn as an alternative to the Star of David and cancels vanilla
 * rendering of the item, the same as the star/custom-texture swap.
 *
 * Edit {@code config/cowhat-bear.json} to authorize players, same format as the other
 * allowlists. Checked by the wearer's own account name/UUID only.
 */
public final class Bear {
    private static final PlayerList LIST = new PlayerList(
            FabricLoader.getInstance().getConfigDir().resolve("cowhat-bear.json"),
            "Add Minecraft usernames and/or UUIDs here to give them a bear above their cow hat.",
            "jagling");

    /** A head circle plus two small ear circles, all outlined, approximated with short polygons. */
    private static final float[] SEGMENTS = Lines.segments(Lines.concat(
            Lines.polygon(0F, 0F, 0.28F, 8),
            Lines.polygon(-0.20F, -0.22F, 0.09F, 6),
            Lines.polygon(0.20F, -0.22F, 0.09F, 6)));

    private Bear() {}

    public static void load() {
        LIST.load();
    }

    /** {@code lowerName} must already be lowercased (see {@link PlayerList#contains}). */
    public static boolean isAuthorized(String lowerName, UUID uuid) {
        return LIST.contains(lowerName, uuid);
    }

    public static void render(PoseStack.Pose pose, VertexConsumer vc) {
        Lines.draw(pose, vc, SEGMENTS, 139, 69, 19); // saddle brown
    }
}
