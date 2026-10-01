package com.celestial878.creamyvanilla.sack.client;

import com.celestial878.creamyvanilla.sack.SackRegistry;
import com.celestial878.creamyvanilla.CreamyVanilla;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CreamyVanilla.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SacksClient {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(SackRegistry.SACK_MENU.get(), SackScreen::new);
    }
}
