package com.cowhat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Hexagram outline (two overlapping equilateral triangles) in Israeli-flag blue. */
public final class StarOfDavid {
    private static final float RADIUS = 0.32F;
    private static final float WIDTH = 5.0F;

    private StarOfDavid() {}

    public static void render(PoseStack.Pose pose, VertexConsumer vc) {
        for (int tri = 0; tri < 2; tri++) {
            float start = tri == 0 ? 90F : 270F;
            for (int i = 0; i < 3; i++) {
                float[] a = point(start + i * 120F), b = point(start + (i + 1) * 120F);
                float dx = b[0] - a[0], dy = b[1] - a[1], len = (float) Math.hypot(dx, dy);
                line(pose, vc, a, b, dx / len, dy / len);
            }
        }
    }

    // Model space has +y pointing down, so negate y to keep the top point up.
    private static float[] point(float deg) {
        double r = Math.toRadians(deg);
        return new float[]{(float) Math.cos(r) * RADIUS, (float) -Math.sin(r) * RADIUS};
    }

    private static void line(PoseStack.Pose pose, VertexConsumer vc, float[] a, float[] b, float nx, float ny) {
        vertex(pose, vc, a, nx, ny);
        vertex(pose, vc, b, nx, ny);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, float[] p, float nx, float ny) {
        vc.addVertex(pose, p[0], p[1], 0F).setColor(0, 56, 184, 255).setNormal(pose, nx, ny, 0F).setLineWidth(WIDTH);
    }
}
