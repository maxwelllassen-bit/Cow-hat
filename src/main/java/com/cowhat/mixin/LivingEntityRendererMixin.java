package com.cowhat.mixin;

import com.cowhat.CowHatState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    /** Flag the render state when the worn head item is a cow hat (the state itself has no item name). */
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void cowhat$flag(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        boolean cow = !head.isEmpty() && head.getHoverName().getString().toLowerCase(Locale.ROOT).contains("cow");
        ((CowHatState) state).cowhat$setCowHat(cow);
    }
}
