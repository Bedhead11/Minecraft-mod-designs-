package com.prisonplanet.client;

import com.prisonplanet.core.ModDimensions;
import com.prisonplanet.core.ModFluids;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionTransitionScreenEvent;

@EventBusSubscriber(modid = "prisonplanet", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(
                ResourceLocation.fromNamespaceAndPath("prisonplanet", "the_condemned"),
                new CondemnedDimensionEffects());
    }

    @SubscribeEvent
    public static void registerTransitionScreens(RegisterDimensionTransitionScreenEvent event) {
        event.registerIncomingEffect(ModDimensions.THE_CONDEMNED_KEY, CondemnedReceivingLevelScreen::new);
        event.registerOutgoingEffect(ModDimensions.THE_CONDEMNED_KEY, CondemnedReceivingLevelScreen::new);
    }

    @SubscribeEvent
    public static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new SuspendedBrineClientExtension(), ModFluids.SUSPENDED_BRINE_TYPE.get());
    }

    @SuppressWarnings("deprecation")
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModFluids.SUSPENDED_BRINE.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_SUSPENDED_BRINE.get(), RenderType.translucent());
        });
    }
}
