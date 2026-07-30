package com.v14d4n.open2online.fabric.client;

import com.v14d4n.open2online.OpenToOnline;
import net.fabricmc.api.ClientModInitializer;

public final class OpenToOnlineFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OpenToOnline.init();
    }
}
