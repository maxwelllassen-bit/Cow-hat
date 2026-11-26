package com.cowhat.mixin;

import com.cowhat.CowHatAuth;
import com.cowhat.CowHatState;
import com.cowhat.CowHatTextures;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    /** Flag the render state from the worn head item's name (the state itself has no item name). */
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void cowhat$flag(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        String name = head.isEmpty() ? null : head.getHoverName().getString();
        boolean cow = name != null && name.toLowerCase(Locale.ROOT).contains("cow");

        // Both the star swap and custom textures are restricted to wearers on the allowlist,
        // checked by the wearer's own account name/UUID (never a token) — i.e. locked to
        // whoever config/cowhat-allowlist.json names (snowrain by default). Anyone running the
        // mod can see it, but only on that wearer's cow hat.
        String wearerName = entity instanceof Player player ? player.getGameProfile().name() : null;
        boolean wearerAuthorized = CowHatAuth.isAuthorized(wearerName, entity.getUUID());

        CowHatState cs = (CowHatState) state;
        cs.cowhat$setCowHat(cow && wearerAuthorized);
        cs.cowhat$setCustomTexture(wearerAuthorized ? CowHatTextures.find(name) : null);
    }
}
