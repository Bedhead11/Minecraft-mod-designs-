package com.prisonplanet.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

/**
 * Shared behavior framework for the nine native predators and wardens.
 *
 * <p>Each concrete type declares behavior through {@link Kind}; a method is
 * used instead of a constructor field because vanilla installs goals while the
 * {@link Monster} superclass constructor is running.</p>
 */
public abstract class CondemnedHostileMob extends Monster implements CondemnedHazardAware {
    public enum Kind {
        ASH_STALKER(false, false),
        CHAIN_WRETCH(false, false),
        FROST_WRAITH(true, true),
        VENT_CRAWLER(false, false),
        CAGE_PHANTOM(false, false),
        CINDER_HOUND(false, false),
        RUST_SENTINEL(false, false),
        SLAG_BRUTE(false, false),
        FURNACE_WARDEN(true, true);

        private final boolean environmentalHazardImmune;
        private final boolean heatproof;

        Kind(boolean environmentalHazardImmune, boolean heatproof) {
            this.environmentalHazardImmune = environmentalHazardImmune;
            this.heatproof = heatproof;
        }

        public boolean isEnvironmentalHazardImmune() {
            return environmentalHazardImmune;
        }

        public boolean isHeatproof() {
            return heatproof;
        }
    }

    protected CondemnedHostileMob(EntityType<? extends CondemnedHostileMob> type, Level level) {
        super(type, level);
    }

    protected abstract Kind kind();

    @Override
    public boolean isEnvironmentalHazardImmune() {
        return kind().isEnvironmentalHazardImmune();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        switch (kind()) {
            case ASH_STALKER -> {
                this.goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.45F));
                this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, true));
            }
            case CHAIN_WRETCH -> {
                this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
                this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.84, true));
            }
            case FROST_WRAITH -> {
                this.goalSelector.addGoal(1, new RestrictSunGoal(this));
                this.goalSelector.addGoal(2, new FleeSunGoal(this, 1.25));
                this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.12, true));
            }
            case VENT_CRAWLER -> {
                this.goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.62F));
                this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.48, false));
            }
            case CAGE_PHANTOM -> {
                this.goalSelector.addGoal(1, new RestrictSunGoal(this));
                this.goalSelector.addGoal(2, new FleeSunGoal(this, 1.2));
                this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
            }
            case CINDER_HOUND -> {
                this.goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.5F));
                this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.42, true));
            }
            case RUST_SENTINEL -> {
                this.goalSelector.addGoal(1, new MoveTowardsRestrictionGoal(this, 1.0));
                this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.96, true));
            }
            case SLAG_BRUTE -> this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.72, true));
            case FURNACE_WARDEN -> {
                this.goalSelector.addGoal(1, new MoveTowardsRestrictionGoal(this, 0.9));
                this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.86, true));
            }
        }
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!super.doHurtTarget(target) || !(target instanceof LivingEntity living)) {
            return false;
        }
        switch (kind()) {
            case CHAIN_WRETCH ->
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
            case FROST_WRAITH -> {
                living.setTicksFrozen(Math.min(140, living.getTicksFrozen() + 80));
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            }
            case VENT_CRAWLER ->
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            case CAGE_PHANTOM ->
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            case CINDER_HOUND -> living.igniteForSeconds(4.0F);
            case RUST_SENTINEL ->
                    living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 0));
            case SLAG_BRUTE -> knockBackTarget(living, 1.0);
            case FURNACE_WARDEN -> {
                living.igniteForSeconds(5.0F);
                knockBackTarget(living, 1.35);
            }
            case ASH_STALKER -> {
                if (this.random.nextFloat() < 0.35F) {
                    living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                }
            }
        }
        return true;
    }

    private void knockBackTarget(LivingEntity target, double power) {
        target.knockback(power, target.getX() - this.getX(), target.getZ() - this.getZ());
    }

    public static AttributeSupplier.Builder attributes(Kind kind) {
        AttributeSupplier.Builder builder = Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 32.0);
        return switch (kind) {
            case ASH_STALKER -> builder
                    .add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.34)
                    .add(Attributes.ATTACK_DAMAGE, 4.0);
            case CHAIN_WRETCH -> builder
                    .add(Attributes.MAX_HEALTH, 28.0).add(Attributes.MOVEMENT_SPEED, 0.21)
                    .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.ARMOR, 3.0);
            case FROST_WRAITH -> builder
                    .add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                    .add(Attributes.ATTACK_DAMAGE, 4.0);
            case VENT_CRAWLER -> builder
                    .add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.39)
                    .add(Attributes.ATTACK_DAMAGE, 3.0);
            case CAGE_PHANTOM -> builder
                    .add(Attributes.MAX_HEALTH, 26.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                    .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.ARMOR, 5.0);
            case CINDER_HOUND -> builder
                    .add(Attributes.MAX_HEALTH, 22.0).add(Attributes.MOVEMENT_SPEED, 0.36)
                    .add(Attributes.ATTACK_DAMAGE, 5.0);
            case RUST_SENTINEL -> builder
                    .add(Attributes.MAX_HEALTH, 34.0).add(Attributes.MOVEMENT_SPEED, 0.23)
                    .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.ARMOR, 9.0);
            case SLAG_BRUTE -> builder
                    .add(Attributes.MAX_HEALTH, 48.0).add(Attributes.MOVEMENT_SPEED, 0.18)
                    .add(Attributes.ATTACK_DAMAGE, 9.0).add(Attributes.ARMOR, 6.0)
                    .add(Attributes.ATTACK_KNOCKBACK, 1.0);
            case FURNACE_WARDEN -> builder
                    .add(Attributes.MAX_HEALTH, 64.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                    .add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.ARMOR, 12.0)
                    .add(Attributes.ATTACK_KNOCKBACK, 1.25);
        };
    }

    public static final class AshStalker extends CondemnedHostileMob {
        public AshStalker(EntityType<? extends AshStalker> type, Level level) { super(type, level); }
        @Override protected Kind kind() { return Kind.ASH_STALKER; }
    }

    public static final class ChainWretch extends CondemnedHostileMob {
        public ChainWretch(EntityType<? extends ChainWretch> type, Level level) { super(type, level); }
        @Override protected Kind kind() { return Kind.CHAIN_WRETCH; }
    }

    public static final class FrostWraith extends CondemnedHostileMob {
        public FrostWraith(EntityType<? extends FrostWraith> type, Level level) { super(type, level); }
        @Override protected Kind kind() { return Kind.FROST_WRAITH; }
    }

    public static final class VentCrawler extends CondemnedHostileMob {
        public VentCrawler(EntityType<? extends VentCrawler> type, Level level) { super(type, level); }
        @Override protected Kind kind() { return Kind.VENT_CRAWLER; }
    }

    public static final class CagePhantom extends CondemnedHostileMob {
        public CagePhantom(EntityType<? extends CagePhantom> type, Level level) { super(type, level); }
        @Override protected Kind kind() { return Kind.CAGE_PHANTOM; }
    }

    public static final class CinderHound extends CondemnedHostileMob {
        public CinderHound(EntityType<? extends CinderHound> type, Level level) { super(type, level); }
        @Override protected Kind kind() { return Kind.CINDER_HOUND; }
    }

    public static final class RustSentinel extends CondemnedHostileMob {
        public RustSentinel(EntityType<? extends RustSentinel> type, Level level) { super(type, level); }
        @Override protected Kind kind() { return Kind.RUST_SENTINEL; }
    }

    public static final class SlagBrute extends CondemnedHostileMob {
        public SlagBrute(EntityType<? extends SlagBrute> type, Level level) { super(type, level); }
        @Override protected Kind kind() { return Kind.SLAG_BRUTE; }
    }

    public static final class FurnaceWarden extends CondemnedHostileMob {
        public FurnaceWarden(EntityType<? extends FurnaceWarden> type, Level level) { super(type, level); }
        @Override protected Kind kind() { return Kind.FURNACE_WARDEN; }
    }
}
