package com.prisonplanet.entity;

import com.prisonplanet.core.ModBlocks;
import com.prisonplanet.core.ModEntities;
import com.prisonplanet.core.ModExpansionBlocks;
import com.prisonplanet.core.ModExpansionItems;
import com.prisonplanet.core.ModItems;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Three native non-hostile species adapted to ashland and prison interiors.
 */
public abstract class CondemnedPassiveMob extends Animal implements CondemnedHazardAware {
    public enum Kind {
        ASH_GRAZER(false),
        GLACIAL_DRIFTER(true),
        SALVAGE_PORTER(false);

        private final boolean environmentalHazardImmune;

        Kind(boolean environmentalHazardImmune) {
            this.environmentalHazardImmune = environmentalHazardImmune;
        }
    }

    protected CondemnedPassiveMob(EntityType<? extends CondemnedPassiveMob> type, Level level) {
        super(type, level);
    }

    protected abstract Kind kind();

    @Override
    public boolean isEnvironmentalHazardImmune() {
        return kind().environmentalHazardImmune;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        switch (kind()) {
            case ASH_GRAZER -> {
                this.goalSelector.addGoal(1, new PanicGoal(this, 1.65));
                this.goalSelector.addGoal(2, new BreedGoal(this, 0.95));
                this.goalSelector.addGoal(3, new TemptGoal(this, 1.15, this::isFood, false));
                this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.05));
                this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.82));
            }
            case GLACIAL_DRIFTER -> {
                this.goalSelector.addGoal(1, new PanicGoal(this, 1.3));
                this.goalSelector.addGoal(2, new BreedGoal(this, 0.72));
                this.goalSelector.addGoal(3, new TemptGoal(this, 0.82, this::isFood, false));
                this.goalSelector.addGoal(4, new FollowParentGoal(this, 0.7));
                this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.6, 120));
            }
            case SALVAGE_PORTER -> {
                this.goalSelector.addGoal(1, new PanicGoal(this, 1.15));
                this.goalSelector.addGoal(2, new BreedGoal(this, 0.82));
                this.goalSelector.addGoal(3, new TemptGoal(this, 0.95, this::isFood, false));
                this.goalSelector.addGoal(4, new FollowParentGoal(this, 0.85));
                this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.65));
            }
        }
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return switch (kind()) {
            case ASH_GRAZER -> stack.is(ModExpansionBlocks.entries().get("fine_ash").get().asItem());
            case GLACIAL_DRIFTER -> stack.is(ModItems.GLACIAL_SHARD.get());
            case SALVAGE_PORTER -> stack.is(ModExpansionItems.FERRIC_SCRAP.get());
        };
    }

    @Nullable
    @Override
    public CondemnedPassiveMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return switch (kind()) {
            case ASH_GRAZER -> ModEntities.ASH_GRAZER.get().create(level);
            case GLACIAL_DRIFTER -> ModEntities.GLACIAL_DRIFTER.get().create(level);
            case SALVAGE_PORTER -> ModEntities.SALVAGE_PORTER.get().create(level);
        };
    }

    public static boolean canSpawn(
            EntityType<? extends CondemnedPassiveMob> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random) {
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        boolean supported = ground.is(ModBlocks.ASHEN_SEDIMENT.get())
                || ModExpansionBlocks.isPlantSoil(ground)
                || ground.isFaceSturdy(level, below, Direction.UP);
        return supported && Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
    }

    public static AttributeSupplier.Builder attributes(Kind kind) {
        AttributeSupplier.Builder builder = Mob.createMobAttributes();
        return switch (kind) {
            case ASH_GRAZER -> builder
                    .add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.22);
            case GLACIAL_DRIFTER -> builder
                    .add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.18)
                    .add(Attributes.FOLLOW_RANGE, 20.0);
            case SALVAGE_PORTER -> builder
                    .add(Attributes.MAX_HEALTH, 26.0).add(Attributes.MOVEMENT_SPEED, 0.17)
                    .add(Attributes.ARMOR, 6.0);
        };
    }

    public static final class AshGrazer extends CondemnedPassiveMob {
        public AshGrazer(EntityType<? extends AshGrazer> type, Level level) { super(type, level); }
        @Override protected Kind kind() { return Kind.ASH_GRAZER; }
    }

    public static final class GlacialDrifter extends CondemnedPassiveMob {
        public GlacialDrifter(EntityType<? extends GlacialDrifter> type, Level level) { super(type, level); }
        @Override protected Kind kind() { return Kind.GLACIAL_DRIFTER; }
    }

    public static final class SalvagePorter extends CondemnedPassiveMob {
        public SalvagePorter(EntityType<? extends SalvagePorter> type, Level level) { super(type, level); }
        @Override protected Kind kind() { return Kind.SALVAGE_PORTER; }
    }
}
