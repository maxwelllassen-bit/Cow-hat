package com.cowhat;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CowHatClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("cowhat");

    @Override
    public void onInitializeClient() {
        // Allowlists and settings are plain files, so load them up front instead of on the
        // first rendered frame. Custom textures stay lazy: they need the GPU context.
        CowHatAuth.load();
        CowHatTextures.loadAllowlist();
        CatEars.load();
        Bear.load();
        HatrixSettings.load();
        HatrixCommand.register();
    }
}
