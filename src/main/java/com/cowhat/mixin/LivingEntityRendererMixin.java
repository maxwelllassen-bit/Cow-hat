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
import java.util.UUID;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    /**
     * Flag the render state from the wearer's allowlists and worn head item's name (the state
     * itself has no item name). Runs for every living entity every frame, so it bails out for
     * non-players before any work, and only builds the item's display name when a cosmetic that
     * depends on it could actually apply.
     */
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void cowhat$flag(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        CowHatState cs = (CowHatState) state;
        // Every allowlist is keyed by a player's own account name/UUID (never a token); only
        // players can be on one. Render states are reused, so always reset.
        if (!(entity instanceof Player player)) {
            cs.cowhat$setCowHat(false);
            cs.cowhat$setCustomTexture(null);
            cs.cowhat$setCatEars(false);
            cs.cowhat$setBear(false);
            return;
        }

        String wearer = player.getGameProfile().name().toLowerCase(Locale.ROOT);
        UUID uuid = entity.getUUID();

        // Each cosmetic has its own, independent allowlist; being on one says nothing about the others.
        boolean starAuth = CowHatAuth.isAuthorized(wearer, uuid);
        boolean textureAuth = CowHatTextures.isAuthorized(wearer, uuid);
        boolean bearAuth = Bear.isAuthorized(wearer, uuid);
        // Cat ears need no particular head item and don't replace one.
        cs.cowhat$setCatEars(CatEars.isAuthorized(wearer, uuid));

        String item = null;
        if (starAuth || textureAuth || bearAuth) {
            ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
            if (!head.isEmpty()) item = head.getHoverName().getString().toLowerCase(Locale.ROOT);
        }
        boolean cow = item != null && item.contains("cow");

        cs.cowhat$setCowHat(cow && starAuth);
        // Custom texture rules can match any item name, not just "cow".
        cs.cowhat$setCustomTexture(textureAuth ? CowHatTextures.find(item) : null);
        // Bear is an alternative to the star, so it also needs a "cow"-named item.
        cs.cowhat$setBear(cow && bearAuth);
    }
}
