package com.cowhat.mixin;

import com.cowhat.CowHatState;
import com.cowhat.CowHatTextures;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements CowHatState {
    private boolean cowhat$cowHat;
    private CowHatTextures.Entry cowhat$customTexture;
    private boolean cowhat$catEars;

    @Override public boolean cowhat$isCowHat() { return cowhat$cowHat; }
    @Override public void cowhat$setCowHat(boolean value) { cowhat$cowHat = value; }

    @Override public CowHatTextures.Entry cowhat$getCustomTexture() { return cowhat$customTexture; }
    @Override public void cowhat$setCustomTexture(CowHatTextures.Entry entry) { cowhat$customTexture = entry; }

    @Override public boolean cowhat$hasCatEars() { return cowhat$catEars; }
    @Override public void cowhat$setCatEars(boolean value) { cowhat$catEars = value; }
}
