package com.prisonplanet.event;

import com.prisonplanet.entity.CondemnedHostileMob;
import com.prisonplanet.entity.CondemnedPassiveMob;
import com.prisonplanet.core.ModEntities;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = "prisonplanet", bus = EventBusSubscriber.Bus.MOD)
public final class ModEntityEvents {
    private ModEntityEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ASH_STALKER.get(), CondemnedHostileMob.attributes(CondemnedHostileMob.Kind.ASH_STALKER).build());
        event.put(ModEntities.CHAIN_WRETCH.get(), CondemnedHostileMob.attributes(CondemnedHostileMob.Kind.CHAIN_WRETCH).build());
        event.put(ModEntities.FROST_WRAITH.get(), CondemnedHostileMob.attributes(CondemnedHostileMob.Kind.FROST_WRAITH).build());
        event.put(ModEntities.VENT_CRAWLER.get(), CondemnedHostileMob.attributes(CondemnedHostileMob.Kind.VENT_CRAWLER).build());
        event.put(ModEntities.CAGE_PHANTOM.get(), CondemnedHostileMob.attributes(CondemnedHostileMob.Kind.CAGE_PHANTOM).build());
        event.put(ModEntities.CINDER_HOUND.get(), CondemnedHostileMob.attributes(CondemnedHostileMob.Kind.CINDER_HOUND).build());
        event.put(ModEntities.RUST_SENTINEL.get(), CondemnedHostileMob.attributes(CondemnedHostileMob.Kind.RUST_SENTINEL).build());
        event.put(ModEntities.SLAG_BRUTE.get(), CondemnedHostileMob.attributes(CondemnedHostileMob.Kind.SLAG_BRUTE).build());
        event.put(ModEntities.FURNACE_WARDEN.get(), CondemnedHostileMob.attributes(CondemnedHostileMob.Kind.FURNACE_WARDEN).build());
        event.put(ModEntities.ASH_GRAZER.get(), CondemnedPassiveMob.attributes(CondemnedPassiveMob.Kind.ASH_GRAZER).build());
        event.put(ModEntities.GLACIAL_DRIFTER.get(), CondemnedPassiveMob.attributes(CondemnedPassiveMob.Kind.GLACIAL_DRIFTER).build());
        event.put(ModEntities.SALVAGE_PORTER.get(), CondemnedPassiveMob.attributes(CondemnedPassiveMob.Kind.SALVAGE_PORTER).build());
    }

    @SubscribeEvent
    public static void registerSpawns(RegisterSpawnPlacementsEvent event) {
        registerDarkHostile(event, ModEntities.ASH_STALKER.get());
        registerDarkHostile(event, ModEntities.CHAIN_WRETCH.get());
        registerDarkHostile(event, ModEntities.FROST_WRAITH.get());
        registerDarkHostile(event, ModEntities.VENT_CRAWLER.get());
        registerDarkHostile(event, ModEntities.CAGE_PHANTOM.get());
        registerDarkHostile(event, ModEntities.CINDER_HOUND.get());

        event.register(ModEntities.RUST_SENTINEL.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SLAG_BRUTE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.FURNACE_WARDEN.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);

        event.register(ModEntities.ASH_GRAZER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CondemnedPassiveMob::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SALVAGE_PORTER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CondemnedPassiveMob::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.GLACIAL_DRIFTER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        CondemnedPassiveMob.canSpawn(type, level, spawnType, pos, random)
                                && Monster.isDarkEnoughToSpawn(level, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static <T extends CondemnedHostileMob> void registerDarkHostile(
            RegisterSpawnPlacementsEvent event, net.minecraft.world.entity.EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
