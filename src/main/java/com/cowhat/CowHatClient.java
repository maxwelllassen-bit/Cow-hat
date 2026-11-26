package com.cowhat;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CowHatClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("cowhat");

    @Override
    public void onInitializeClient() {
        CowHatAuth.load();
    }

    /** Whether the star-swap feature should run right now: gated to allowlisted local players. */
    public static boolean enabled() {
        net.minecraft.client.User user = Minecraft.getInstance().getUser();
        if (user == null) return false;
        return CowHatAuth.isLocalPlayerAuthorized(user.getName(), user.getProfileId());
    }
}
