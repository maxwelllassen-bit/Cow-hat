package com.cowhat.mixin;

import com.cowhat.CowHatState;
import com.cowhat.CowHatTextures;
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
     * Replace a matching head item with either a configured custom texture or, failing that,
     * the built-in Star of David, floating above the head. Both flags on the render state are
     * already restricted to allowlisted wearers (see {@link com.cowhat.CowHatAuth}); this just
     * draws whichever one is set, for any viewer running the mod.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void cowhat$submit(PoseStack pose, SubmitNodeCollector collector, int light, LivingEntityRenderState state,
                               float yRot, float xRot, CallbackInfo ci) {
        CowHatState cs = (CowHatState) state;
        boolean isCowHat = cs.cowhat$isCowHat();
        CowHatTextures.Entry custom = cs.cowhat$getCustomTexture();
        if (!isCowHat && custom == null) return;
        ci.cancel();

        EntityModel model = ((RenderLayer) (Object) this).getParentModel();
        pose.pushPose();
        model.root().translateAndRotate(pose);
        ((HeadedModel) model).translateToHead(pose);
        pose.translate(0.0F, -0.95F, 0.0F); // model space: -y is up; head top is at -0.5

        if (custom != null) {
            collector.submitCustomGeometry(pose, custom.renderType(), (p, vc) -> CowHatTextures.renderQuad(p, vc, custom, light));
        } else {
            collector.submitCustomGeometry(pose, RenderTypes.lines(), StarOfDavid::render);
        }
        pose.popPose();
    }
}
