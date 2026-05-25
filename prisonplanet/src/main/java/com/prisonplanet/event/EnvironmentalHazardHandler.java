package com.prisonplanet.event;

import com.prisonplanet.core.ModDimensions;
import com.prisonplanet.dimension.CyclePhase;
import com.prisonplanet.dimension.PrisonPlanetSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Set;

/**
 * Applies environmental hazards to players in The Condemned dimension.
 *
 * DAY hazard: players exposed to sky at Y >= 50 without Fire Resistance catch fire every 40 ticks.
 * NIGHT hazard: players exposed to sky at Y >= 50 freeze unless near a heat source.
 */
@EventBusSubscriber(modid = "prisonplanet", bus = EventBusSubscriber.Bus.GAME)
public final class EnvironmentalHazardHandler {

    /** Blocks that count as heat sources for the night hazard. */
    private static final Set<net.minecraft.world.level.block.Block> HEAT_BLOCKS = Set.of(
            Blocks.CAMPFIRE,
            Blocks.SOUL_CAMPFIRE,
            Blocks.FIRE,
            Blocks.SOUL_FIRE,
            Blocks.FURNACE,
            Blocks.SMOKER,
            Blocks.BLAST_FURNACE,
            Blocks.LAVA
    );

    /** Blocks whose heat only applies when the LIT blockstate is true. */
    private static final Set<net.minecraft.world.level.block.Block> NEEDS_LIT = Set.of(
            Blocks.CAMPFIRE,
            Blocks.SOUL_CAMPFIRE,
            Blocks.FURNACE,
            Blocks.SMOKER,
            Blocks.BLAST_FURNACE
    );

    private static final int HEAT_SEARCH_RADIUS = 5;
    private static final int MAX_FREEZE_TICKS = 300;

    private EnvironmentalHazardHandler() {}

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var level = player.level();
        if (!level.dimension().equals(ModDimensions.THE_CONDEMNED_KEY)) return;

        if (!(level instanceof ServerLevel serverLevel)) return;

        PrisonPlanetSavedData data = PrisonPlanetSavedData.getOrCreate(serverLevel);
        CyclePhase phase = data.getCurrentPhase();

        switch (phase) {
            case DAY -> handleDayHazard(player);
            case NIGHT -> handleNightHazard(player);
            default -> clearFreezeGradually(player); // SUNSET, SUNRISE — neither hazard active
        }
    }

    // -------------------------------------------------------------------------
    // Hazard logic
    // -------------------------------------------------------------------------

    private static void handleDayHazard(ServerPlayer player) {
        if (!isExposedToHazard(player)) return;
        // No hazard if player has Fire Resistance
        if (player.hasEffect(MobEffects.FIRE_RESISTANCE)) return;
        // Set on fire every 40 ticks
        if (player.tickCount % 40 == 0) {
            player.igniteForSeconds(3);
        }
    }

    private static void handleNightHazard(ServerPlayer player) {
        if (!isExposedToHazard(player)) {
            // Not exposed — clear freeze gradually
            clearFreezeGradually(player);
            return;
        }

        if (hasNearbyHeatSource(player)) {
            // Near a heat source — thaw out
            player.setTicksFrozen(Math.max(0, player.getTicksFrozen() - 4));
        } else {
            // No heat — freeze the player
            int newFreeze = Math.min(MAX_FREEZE_TICKS, player.getTicksFrozen() + 8);
            player.setTicksFrozen(newFreeze);
        }
    }

    private static void clearFreezeGradually(ServerPlayer player) {
        if (player.getTicksFrozen() > 0) {
            player.setTicksFrozen(Math.max(0, player.getTicksFrozen() - 8));
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    /**
     * Returns true if the player is at Y >= 50 AND has sky light level >= 7.
     */
    private static boolean isExposedToHazard(ServerPlayer player) {
        if (player.getBlockY() < 50) return false;
        int skyLight = player.level().getBrightness(LightLayer.SKY, player.blockPosition());
        return skyLight >= 7;
    }

    /**
     * Scans a 5-block sphere around the player for a valid heat source block.
     */
    private static boolean hasNearbyHeatSource(ServerPlayer player) {
        BlockPos center = player.blockPosition();
        int r = HEAT_SEARCH_RADIUS;

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    // Sphere check (Manhattan vs Euclidean — spec says "5-block sphere", use Euclidean-ish)
                    if (dx * dx + dy * dy + dz * dz > r * r) continue;

                    BlockPos checkPos = center.offset(dx, dy, dz);
                    BlockState state = player.level().getBlockState(checkPos);
                    var block = state.getBlock();

                    if (!HEAT_BLOCKS.contains(block)) continue;

                    // For blocks that need to be lit, check the LIT property
                    if (NEEDS_LIT.contains(block)) {
                        if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) {
                            continue; // Not lit, doesn't count
                        }
                    }

                    return true;
                }
            }
        }
        return false;
    }
}
