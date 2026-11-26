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

    private static final float WIDTH = 5.0F;
    private static final int R = 139, G = 69, B = 19; // saddle brown

    private Bear() {}

    public static boolean isAuthorized(String name, UUID uuid) {
        return LIST.contains(name, uuid);
    }

    /** A head circle plus two small ear circles, all outlined, approximated with short polygons. */
    public static void render(PoseStack.Pose pose, VertexConsumer vc) {
        polygon(pose, vc, 0F, 0F, 0.28F, 8);
        polygon(pose, vc, -0.20F, -0.22F, 0.09F, 6);
        polygon(pose, vc, 0.20F, -0.22F, 0.09F, 6);
    }

    private static void polygon(PoseStack.Pose pose, VertexConsumer vc, float cx, float cy, float radius, int sides) {
        float[][] points = new float[sides][2];
        for (int i = 0; i < sides; i++) {
            double a = 2 * Math.PI * i / sides;
            points[i][0] = cx + (float) Math.cos(a) * radius;
            points[i][1] = cy + (float) Math.sin(a) * radius;
        }
        for (int i = 0; i < sides; i++) {
            edge(pose, vc, points[i], points[(i + 1) % sides]);
        }
    }

    private static void edge(PoseStack.Pose pose, VertexConsumer vc, float[] a, float[] b) {
        float dx = b[0] - a[0], dy = b[1] - a[1];
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float nx = dx / len, ny = dy / len;
        vertex(pose, vc, a, nx, ny);
        vertex(pose, vc, b, nx, ny);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, float[] p, float nx, float ny) {
        vc.addVertex(pose, p[0], p[1], 0F).setColor(R, G, B, 255).setNormal(pose, nx, ny, 0F).setLineWidth(WIDTH);
    }
}
