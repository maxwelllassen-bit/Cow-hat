package com.cowhat.mixin;

import com.cowhat.CowHatState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements CowHatState {
    private boolean cowhat$cowHat;

    @Override public boolean cowhat$isCowHat() { return cowhat$cowHat; }
    @Override public void cowhat$setCowHat(boolean value) { cowhat$cowHat = value; }
}
