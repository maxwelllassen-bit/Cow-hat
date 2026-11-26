package com.cowhat;

/** Duck interface added to LivingEntityRenderState: flags set from the worn head item's name. */
public interface CowHatState {
    boolean cowhat$isCowHat();
    void cowhat$setCowHat(boolean value);

    /** The matching {@link CowHatTextures.Entry}, or null if no custom texture rule matched. */
    CowHatTextures.Entry cowhat$getCustomTexture();
    void cowhat$setCustomTexture(CowHatTextures.Entry entry);

    /** True if this entity is on the cat-ears allowlist ({@link CatEars}). */
    boolean cowhat$hasCatEars();
    void cowhat$setCatEars(boolean value);
}
