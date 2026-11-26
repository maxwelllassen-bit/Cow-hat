package com.cowhat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.loader.api.FabricLoader;

import java.util.UUID;

/**
 * Gives an allowlisted player a pair of cat ears, drawn on top of their head regardless of
 * what they're wearing (it does not replace anything, unlike the star/custom-texture swap).
 *
 * Edit {@code config/cowhat-catears.json} to authorize players, same format as the main
 * allowlist. Checked by the wearer's own account name/UUID only.
 */
public final class CatEars {
    private static final PlayerList LIST = new PlayerList(
            FabricLoader.getInstance().getConfigDir().resolve("cowhat-catears.json"),
            "Add Minecraft usernames and/or UUIDs here to give them cat ears.",
            "Becausepainfully");

    private static final float WIDTH = 5.0F;

    private CatEars() {}

    public static boolean isAuthorized(String name, UUID uuid) {
        return LIST.contains(name, uuid);
    }

    /**
     * Draws two black triangle-outline ears on top of the head. Called with the pose already
     * translated to the head's pivot (model space: -y is up, head top is at y = -0.5).
     */
    public static void render(PoseStack.Pose pose, VertexConsumer vc) {
        ear(pose, vc, 1F);  // right ear
        ear(pose, vc, -1F); // left ear (mirrored)
    }

    private static void ear(PoseStack.Pose pose, VertexConsumer vc, float side) {
        float[] baseNear = {side * 0.12F, -0.5F};
        float[] baseFar = {side * 0.32F, -0.5F};
        float[] apex = {side * 0.22F, -0.75F};
        edge(pose, vc, baseNear, baseFar);
        edge(pose, vc, baseFar, apex);
        edge(pose, vc, apex, baseNear);
    }

    private static void edge(PoseStack.Pose pose, VertexConsumer vc, float[] a, float[] b) {
        float dx = b[0] - a[0], dy = b[1] - a[1];
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float nx = dx / len, ny = dy / len;
        vertex(pose, vc, a, nx, ny);
        vertex(pose, vc, b, nx, ny);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, float[] p, float nx, float ny) {
        vc.addVertex(pose, p[0], p[1], 0F).setColor(0, 0, 0, 255).setNormal(pose, nx, ny, 0F).setLineWidth(WIDTH);
    }
}
