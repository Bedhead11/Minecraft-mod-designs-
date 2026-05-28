package com.prisonplanet.client;

import com.prisonplanet.client.model.CondemnedCreatureModel;
import com.prisonplanet.client.renderer.CondemnedMobRenderer;
import com.prisonplanet.core.ModEntities;
import com.prisonplanet.entity.CondemnedHostileMob;
import com.prisonplanet.entity.CondemnedPassiveMob;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = "prisonplanet", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntityClientEvents {
    private static final ModelLayerLocation ASH_STALKER = layer("ash_stalker");
    private static final ModelLayerLocation CHAIN_WRETCH = layer("chain_wretch");
    private static final ModelLayerLocation FROST_WRAITH = layer("frost_wraith");
    private static final ModelLayerLocation VENT_CRAWLER = layer("vent_crawler");
    private static final ModelLayerLocation CAGE_PHANTOM = layer("cage_phantom");
    private static final ModelLayerLocation CINDER_HOUND = layer("cinder_hound");
    private static final ModelLayerLocation RUST_SENTINEL = layer("rust_sentinel");
    private static final ModelLayerLocation SLAG_BRUTE = layer("slag_brute");
    private static final ModelLayerLocation FURNACE_WARDEN = layer("furnace_warden");
    private static final ModelLayerLocation ASH_GRAZER = layer("ash_grazer");
    private static final ModelLayerLocation GLACIAL_DRIFTER = layer("glacial_drifter");
    private static final ModelLayerLocation SALVAGE_PORTER = layer("salvage_porter");

    private ModEntityClientEvents() {
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ASH_STALKER, CondemnedCreatureModel::createAshStalkerLayer);
        event.registerLayerDefinition(CHAIN_WRETCH, CondemnedCreatureModel::createChainWretchLayer);
        event.registerLayerDefinition(FROST_WRAITH, CondemnedCreatureModel::createFrostWraithLayer);
        event.registerLayerDefinition(VENT_CRAWLER, CondemnedCreatureModel::createVentCrawlerLayer);
        event.registerLayerDefinition(CAGE_PHANTOM, CondemnedCreatureModel::createCagePhantomLayer);
        event.registerLayerDefinition(CINDER_HOUND, CondemnedCreatureModel::createCinderHoundLayer);
        event.registerLayerDefinition(RUST_SENTINEL, CondemnedCreatureModel::createRustSentinelLayer);
        event.registerLayerDefinition(SLAG_BRUTE, CondemnedCreatureModel::createSlagBruteLayer);
        event.registerLayerDefinition(FURNACE_WARDEN, CondemnedCreatureModel::createFurnaceWardenLayer);
        event.registerLayerDefinition(ASH_GRAZER, CondemnedCreatureModel::createAshGrazerLayer);
        event.registerLayerDefinition(GLACIAL_DRIFTER, CondemnedCreatureModel::createGlacialDrifterLayer);
        event.registerLayerDefinition(SALVAGE_PORTER, CondemnedCreatureModel::createSalvagePorterLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ASH_STALKER.get(), context ->
                new CondemnedMobRenderer<CondemnedHostileMob.AshStalker>(context, ASH_STALKER, "ash_stalker", 0.42F));
        event.registerEntityRenderer(ModEntities.CHAIN_WRETCH.get(), context ->
                new CondemnedMobRenderer<CondemnedHostileMob.ChainWretch>(context, CHAIN_WRETCH, "chain_wretch", 0.45F));
        event.registerEntityRenderer(ModEntities.FROST_WRAITH.get(), context ->
                new CondemnedMobRenderer<CondemnedHostileMob.FrostWraith>(context, FROST_WRAITH, "frost_wraith", 0.45F));
        event.registerEntityRenderer(ModEntities.VENT_CRAWLER.get(), context ->
                new CondemnedMobRenderer<CondemnedHostileMob.VentCrawler>(context, VENT_CRAWLER, "vent_crawler", 0.35F));
        event.registerEntityRenderer(ModEntities.CAGE_PHANTOM.get(), context ->
                new CondemnedMobRenderer<CondemnedHostileMob.CagePhantom>(context, CAGE_PHANTOM, "cage_phantom", 0.48F));
        event.registerEntityRenderer(ModEntities.CINDER_HOUND.get(), context ->
                new CondemnedMobRenderer<CondemnedHostileMob.CinderHound>(context, CINDER_HOUND, "cinder_hound", 0.45F));
        event.registerEntityRenderer(ModEntities.RUST_SENTINEL.get(), context ->
                new CondemnedMobRenderer<CondemnedHostileMob.RustSentinel>(context, RUST_SENTINEL, "rust_sentinel", 0.5F));
        event.registerEntityRenderer(ModEntities.SLAG_BRUTE.get(), context ->
                new CondemnedMobRenderer<CondemnedHostileMob.SlagBrute>(context, SLAG_BRUTE, "slag_brute", 0.72F));
        event.registerEntityRenderer(ModEntities.FURNACE_WARDEN.get(), context ->
                new CondemnedMobRenderer<CondemnedHostileMob.FurnaceWarden>(context, FURNACE_WARDEN, "furnace_warden", 0.8F));
        event.registerEntityRenderer(ModEntities.ASH_GRAZER.get(), context ->
                new CondemnedMobRenderer<CondemnedPassiveMob.AshGrazer>(context, ASH_GRAZER, "ash_grazer", 0.5F));
        event.registerEntityRenderer(ModEntities.GLACIAL_DRIFTER.get(), context ->
                new CondemnedMobRenderer<CondemnedPassiveMob.GlacialDrifter>(context, GLACIAL_DRIFTER, "glacial_drifter", 0.35F));
        event.registerEntityRenderer(ModEntities.SALVAGE_PORTER.get(), context ->
                new CondemnedMobRenderer<CondemnedPassiveMob.SalvagePorter>(context, SALVAGE_PORTER, "salvage_porter", 0.58F));
    }

    private static ModelLayerLocation layer(String id) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("prisonplanet", id), "main");
    }
}
