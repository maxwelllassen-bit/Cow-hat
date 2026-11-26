package com.cowhat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Hexagram outline (two overlapping equilateral triangles) in Israeli-flag blue. */
public final class StarOfDavid {
    private static final float RADIUS = 0.32F;
    private static final float[] SEGMENTS = build();

    private StarOfDavid() {}

    public static void render(PoseStack.Pose pose, VertexConsumer vc) {
        Lines.draw(pose, vc, SEGMENTS, 0, 56, 184);
    }

    private static float[] build() {
        float[] xy = new float[2 * 3 * 4];
        int k = 0;
        for (int tri = 0; tri < 2; tri++) {
            float start = tri == 0 ? 90F : 270F;
            for (int i = 0; i < 3; i++) {
                double a = Math.toRadians(start + i * 120F), b = Math.toRadians(start + (i + 1) * 120F);
                // Model space has +y pointing down, so negate y to keep the top point up.
                xy[k++] = (float) Math.cos(a) * RADIUS;
                xy[k++] = (float) -Math.sin(a) * RADIUS;
                xy[k++] = (float) Math.cos(b) * RADIUS;
                xy[k++] = (float) -Math.sin(b) * RADIUS;
            }
        }
        return Lines.segments(xy);
    }
}
