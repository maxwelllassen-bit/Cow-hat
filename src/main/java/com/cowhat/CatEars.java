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

    /** Two triangle-outline ears (model space: -y is up, head top is at y = -0.5). */
    private static final float[] SEGMENTS = Lines.segments(Lines.concat(ear(1F), ear(-1F)));

    private CatEars() {}

    public static void load() {
        LIST.load();
    }

    /** {@code lowerName} must already be lowercased (see {@link PlayerList#contains}). */
    public static boolean isAuthorized(String lowerName, UUID uuid) {
        return LIST.contains(lowerName, uuid);
    }

    /** Called with the pose already translated to the head's pivot. */
    public static void render(PoseStack.Pose pose, VertexConsumer vc) {
        Lines.draw(pose, vc, SEGMENTS, 0, 0, 0);
    }

    private static float[] ear(float side) {
        float nearX = side * 0.12F, farX = side * 0.32F, apexX = side * 0.22F;
        float baseY = -0.5F, apexY = -0.75F;
        return new float[]{
                nearX, baseY, farX, baseY,
                farX, baseY, apexX, apexY,
                apexX, apexY, nearX, baseY,
        };
    }
}
