package com.prisonplanet.core;

import com.prisonplanet.entity.CondemnedHostileMob;
import com.prisonplanet.entity.CondemnedPassiveMob;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, "prisonplanet");

    public static final DeferredHolder<EntityType<?>, EntityType<CondemnedHostileMob.AshStalker>> ASH_STALKER =
            ENTITIES.register("ash_stalker", () -> EntityType.Builder
                    .of(CondemnedHostileMob.AshStalker::new, MobCategory.MONSTER)
                    .sized(0.65F, 1.55F).clientTrackingRange(8).build("prisonplanet:ash_stalker"));
    public static final DeferredHolder<EntityType<?>, EntityType<CondemnedHostileMob.ChainWretch>> CHAIN_WRETCH =
            ENTITIES.register("chain_wretch", () -> EntityType.Builder
                    .of(CondemnedHostileMob.ChainWretch::new, MobCategory.MONSTER)
                    .sized(0.72F, 2.05F).clientTrackingRange(8).build("prisonplanet:chain_wretch"));
    public static final DeferredHolder<EntityType<?>, EntityType<CondemnedHostileMob.FrostWraith>> FROST_WRAITH =
            ENTITIES.register("frost_wraith", () -> EntityType.Builder
                    .of(CondemnedHostileMob.FrostWraith::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.9F).clientTrackingRange(10).build("prisonplanet:frost_wraith"));
    public static final DeferredHolder<EntityType<?>, EntityType<CondemnedHostileMob.VentCrawler>> VENT_CRAWLER =
            ENTITIES.register("vent_crawler", () -> EntityType.Builder
                    .of(CondemnedHostileMob.VentCrawler::new, MobCategory.MONSTER)
                    .sized(0.82F, 0.65F).clientTrackingRange(8).build("prisonplanet:vent_crawler"));
    public static final DeferredHolder<EntityType<?>, EntityType<CondemnedHostileMob.CagePhantom>> CAGE_PHANTOM =
            ENTITIES.register("cage_phantom", () -> EntityType.Builder
                    .of(CondemnedHostileMob.CagePhantom::new, MobCategory.MONSTER)
                    .sized(0.85F, 1.85F).clientTrackingRange(10).build("prisonplanet:cage_phantom"));
    public static final DeferredHolder<EntityType<?>, EntityType<CondemnedHostileMob.CinderHound>> CINDER_HOUND =
            ENTITIES.register("cinder_hound", () -> EntityType.Builder
                    .of(CondemnedHostileMob.CinderHound::new, MobCategory.MONSTER)
                    .sized(0.85F, 0.95F).clientTrackingRange(8).build("prisonplanet:cinder_hound"));
    public static final DeferredHolder<EntityType<?>, EntityType<CondemnedHostileMob.RustSentinel>> RUST_SENTINEL =
            ENTITIES.register("rust_sentinel", () -> EntityType.Builder
                    .of(CondemnedHostileMob.RustSentinel::new, MobCategory.MONSTER)
                    .sized(0.78F, 2.0F).clientTrackingRange(10).build("prisonplanet:rust_sentinel"));
    public static final DeferredHolder<EntityType<?>, EntityType<CondemnedHostileMob.SlagBrute>> SLAG_BRUTE =
            ENTITIES.register("slag_brute", () -> EntityType.Builder
                    .of(CondemnedHostileMob.SlagBrute::new, MobCategory.MONSTER)
                    .sized(1.2F, 2.25F).clientTrackingRange(10).build("prisonplanet:slag_brute"));
    public static final DeferredHolder<EntityType<?>, EntityType<CondemnedHostileMob.FurnaceWarden>> FURNACE_WARDEN =
            ENTITIES.register("furnace_warden", () -> EntityType.Builder
                    .of(CondemnedHostileMob.FurnaceWarden::new, MobCategory.MONSTER)
                    .sized(1.22F, 2.65F).fireImmune().clientTrackingRange(12)
                    .build("prisonplanet:furnace_warden"));

    public static final DeferredHolder<EntityType<?>, EntityType<CondemnedPassiveMob.AshGrazer>> ASH_GRAZER =
            ENTITIES.register("ash_grazer", () -> EntityType.Builder
                    .of(CondemnedPassiveMob.AshGrazer::new, MobCategory.CREATURE)
                    .sized(0.92F, 1.25F).clientTrackingRange(8).build("prisonplanet:ash_grazer"));
    public static final DeferredHolder<EntityType<?>, EntityType<CondemnedPassiveMob.GlacialDrifter>> GLACIAL_DRIFTER =
            ENTITIES.register("glacial_drifter", () -> EntityType.Builder
                    .of(CondemnedPassiveMob.GlacialDrifter::new, MobCategory.CREATURE)
                    .sized(0.8F, 0.75F).clientTrackingRange(10).build("prisonplanet:glacial_drifter"));
    public static final DeferredHolder<EntityType<?>, EntityType<CondemnedPassiveMob.SalvagePorter>> SALVAGE_PORTER =
            ENTITIES.register("salvage_porter", () -> EntityType.Builder
                    .of(CondemnedPassiveMob.SalvagePorter::new, MobCategory.CREATURE)
                    .sized(1.05F, 1.0F).clientTrackingRange(8).build("prisonplanet:salvage_porter"));

    private ModEntities() {
    }
}
