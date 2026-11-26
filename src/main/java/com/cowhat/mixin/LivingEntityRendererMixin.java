package com.cowhat.mixin;

import com.cowhat.Bear;
import com.cowhat.CatEars;
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

        // The star swap is restricted to wearers on cowhat-allowlist.json, checked by the
        // wearer's own account name/UUID (never a token). Anyone running the mod can see it,
        // but only on that wearer's cow hat.
        String wearerName = entity instanceof Player player ? player.getGameProfile().name() : null;
        java.util.UUID wearerUuid = entity.getUUID();
        boolean wearerAuthorized = CowHatAuth.isAuthorized(wearerName, wearerUuid);

        CowHatState cs = (CowHatState) state;
        cs.cowhat$setCowHat(cow && wearerAuthorized);

        // Custom textures are gated by their OWN, separate allowlist (cowhat-textures-allowlist.json),
        // independent of cowhat-allowlist.json above — a wearer doesn't need the star allowlist
        // to get a custom texture, and a rule can match any item name, not just "cow".
        cs.cowhat$setCustomTexture(CowHatTextures.isAuthorized(wearerName, wearerUuid) ? CowHatTextures.find(name) : null);

        // Cat ears are a separate cosmetic with its own allowlist (cowhat-catears.json);
        // they don't require any particular head item and don't replace one.
        cs.cowhat$setCatEars(CatEars.isAuthorized(wearerName, wearerUuid));

        // Bear is also a separate, independently-allowlisted cosmetic (cowhat-bear.json,
        // jagling by default), but unlike cat ears it only applies while wearing a "cow"-named
        // item — it's an alternative to the star, not an unconditional addition.
        cs.cowhat$setBear(cow && Bear.isAuthorized(wearerName, wearerUuid));
    }
}
