package com.cowhat.mixin;

import com.cowhat.Bear;
import com.cowhat.CatEars;
import com.cowhat.CowHatState;
import com.cowhat.CowHatTextures;
import com.cowhat.HatrixSettings;
import com.cowhat.StarOfDavid;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin {
    /** Model space: -y is up; the head top is at -0.5, so this floats the icon just above it. */
    private static final float ICON_Y = -0.95F;

    /**
     * Independent kinds of cosmetic, each restricted to its own allowlisted wearers (see
     * {@link com.cowhat.CowHatAuth}, {@link CowHatTextures}, {@link CatEars}, {@link Bear}),
     * visible to any viewer running the mod:
     * - the head-item swap (custom texture, else the bear, else the Star of David) replaces a
     *   matching item entirely, cancelling vanilla rendering for it — UNLESS {@code /hatrix
     *   toggle} has switched custom textures to overlay mode, in which case a matched texture
     *   is drawn additively instead, and the real item is left alone (or still replaced by the
     *   bear/star, if either of those also applies to that wearer);
     * - cat ears are always drawn additively on top of whatever's actually worn.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void cowhat$submit(PoseStack pose, SubmitNodeCollector collector, int light, LivingEntityRenderState state,
                               float yRot, float xRot, CallbackInfo ci) {
        CowHatState cs = (CowHatState) state;
        CowHatTextures.Entry custom = cs.cowhat$getCustomTexture();
        boolean bear = cs.cowhat$hasBear();
        boolean star = cs.cowhat$isCowHat();
        boolean ears = cs.cowhat$hasCatEars();
        if (custom == null && !bear && !star && !ears) return; // fast path: nothing of ours on this entity

        boolean overlayTexture = custom != null && HatrixSettings.isCustomTextureOverlay();
        boolean replaceTexture = custom != null && !overlayTexture;
        boolean swap = replaceTexture || bear || star;

        EntityModel model = ((RenderLayer) (Object) this).getParentModel();
        pose.pushPose();
        model.root().translateAndRotate(pose);
        ((HeadedModel) model).translateToHead(pose);

        if (swap) {
            ci.cancel(); // replacing the head item entirely: skip vanilla rendering of it
            if (!replaceTexture) {
                pose.pushPose();
                pose.translate(0.0F, ICON_Y, 0.0F);
                collector.submitCustomGeometry(pose, RenderTypes.lines(), bear ? Bear::render : StarOfDavid::render);
                pose.popPose();
            }
        }
        if (custom != null) { // replace mode drew nothing above, so either mode lands here once
            pose.pushPose();
            pose.translate(0.0F, ICON_Y, 0.0F);
            collector.submitCustomGeometry(pose, custom.renderType(), (p, vc) -> CowHatTextures.renderQuad(p, vc, custom, light));
            pose.popPose();
        }
        if (ears) {
            collector.submitCustomGeometry(pose, RenderTypes.lines(), CatEars::render);
        }
        pose.popPose();
    }
}
