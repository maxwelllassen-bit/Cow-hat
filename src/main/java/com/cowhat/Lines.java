package com.cowhat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Shared line-outline drawing for the line-based cosmetics (star, bear, cat ears). Shapes are
 * built once into a flat array of segments, so per-frame rendering does no trig and no
 * allocation — just one vertex pair per segment.
 */
final class Lines {
    private static final float WIDTH = 5.0F;
    /** Floats per stored segment: x1, y1, x2, y2, nx, ny. */
    private static final int STRIDE = 6;

    private Lines() {}

    /** Builds a segment array from raw {x1, y1, x2, y2} groups, precomputing each segment's normal. */
    static float[] segments(float... xy) {
        if (xy.length % 4 != 0) throw new IllegalArgumentException("segments need x1,y1,x2,y2 groups");
        int n = xy.length / 4;
        float[] out = new float[n * STRIDE];
        for (int i = 0; i < n; i++) {
            float x1 = xy[i * 4], y1 = xy[i * 4 + 1], x2 = xy[i * 4 + 2], y2 = xy[i * 4 + 3];
            float dx = x2 - x1, dy = y2 - y1;
            float len = (float) Math.hypot(dx, dy);
            int o = i * STRIDE;
            out[o] = x1;
            out[o + 1] = y1;
            out[o + 2] = x2;
            out[o + 3] = y2;
            out[o + 4] = dx / len;
            out[o + 5] = dy / len;
        }
        return out;
    }

    /** Raw {x1, y1, x2, y2} groups for a closed regular polygon's outline. */
    static float[] polygon(float cx, float cy, float radius, int sides) {
        float[] xy = new float[sides * 4];
        for (int i = 0; i < sides; i++) {
            double a = 2 * Math.PI * i / sides, b = 2 * Math.PI * (i + 1) / sides;
            xy[i * 4] = cx + (float) Math.cos(a) * radius;
            xy[i * 4 + 1] = cy + (float) Math.sin(a) * radius;
            xy[i * 4 + 2] = cx + (float) Math.cos(b) * radius;
            xy[i * 4 + 3] = cy + (float) Math.sin(b) * radius;
        }
        return xy;
    }

    static float[] concat(float[]... parts) {
        int len = 0;
        for (float[] p : parts) len += p.length;
        float[] out = new float[len];
        int o = 0;
        for (float[] p : parts) {
            System.arraycopy(p, 0, out, o, p.length);
            o += p.length;
        }
        return out;
    }

    static void draw(PoseStack.Pose pose, VertexConsumer vc, float[] segs, int r, int g, int b) {
        for (int o = 0; o < segs.length; o += STRIDE) {
            float nx = segs[o + 4], ny = segs[o + 5];
            vc.addVertex(pose, segs[o], segs[o + 1], 0F).setColor(r, g, b, 255).setNormal(pose, nx, ny, 0F).setLineWidth(WIDTH);
            vc.addVertex(pose, segs[o + 2], segs[o + 3], 0F).setColor(r, g, b, 255).setNormal(pose, nx, ny, 0F).setLineWidth(WIDTH);
        }
    }
}
