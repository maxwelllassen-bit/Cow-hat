package com.cowhat;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CowHatClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("cowhat");

    @Override
    public void onInitializeClient() {
        CowHatAuth.load();
        HatrixCommand.register();
    }
}
