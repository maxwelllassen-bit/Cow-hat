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
    /**
     * Three independent kinds of cosmetic, each restricted to its own allowlisted wearers (see
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
        boolean overlayTexture = custom != null && HatrixSettings.isCustomTextureOverlay();
        boolean replaceTexture = custom != null && !overlayTexture;
        boolean bear = cs.cowhat$hasBear();
        boolean swap = cs.cowhat$isCowHat() || replaceTexture || bear;
        boolean ears = cs.cowhat$hasCatEars();
        if (!swap && !ears && !overlayTexture) return;

        EntityModel model = ((RenderLayer) (Object) this).getParentModel();
        pose.pushPose();
        model.root().translateAndRotate(pose);
        ((HeadedModel) model).translateToHead(pose);

        if (swap) {
            ci.cancel(); // replacing the head item entirely: skip vanilla rendering of it
            pose.pushPose();
            pose.translate(0.0F, -0.95F, 0.0F); // model space: -y is up; head top is at -0.5
            if (replaceTexture) {
                collector.submitCustomGeometry(pose, custom.renderType(), (p, vc) -> CowHatTextures.renderQuad(p, vc, custom, light));
            } else if (bear) {
                collector.submitCustomGeometry(pose, RenderTypes.lines(), Bear::render);
            } else {
                collector.submitCustomGeometry(pose, RenderTypes.lines(), StarOfDavid::render);
            }
            pose.popPose();
        }
        if (overlayTexture) {
            pose.pushPose();
            pose.translate(0.0F, -0.95F, 0.0F);
            collector.submitCustomGeometry(pose, custom.renderType(), (p, vc) -> CowHatTextures.renderQuad(p, vc, custom, light));
            pose.popPose();
        }
        if (ears) {
            collector.submitCustomGeometry(pose, RenderTypes.lines(), CatEars::render);
        }
        pose.popPose();
    }
}
