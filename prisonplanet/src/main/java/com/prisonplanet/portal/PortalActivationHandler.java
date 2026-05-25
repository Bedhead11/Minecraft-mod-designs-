package com.prisonplanet.portal;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = "prisonplanet", bus = EventBusSubscriber.Bus.GAME)
public final class PortalActivationHandler {

    private PortalActivationHandler() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;

        var player = event.getEntity();
        var hand = event.getHand();
        var stack = player.getItemInHand(hand);
        var item = stack.getItem();

        if (item != Items.FLINT_AND_STEEL && item != Items.FIRE_CHARGE) return;

        var clickedPos = event.getPos();
        var level = event.getLevel();
        if (!level.getBlockState(clickedPos).is(Blocks.NETHERITE_BLOCK)) return;

        // CondemnedPortalShape expects an interior (air) position, not the frame block.
        // Try all 6 adjacent positions — one of them should be inside the portal frame.
        boolean spawned = false;
        for (Direction dir : Direction.values()) {
            BlockPos adjacent = clickedPos.relative(dir);
            BlockState adjacentState = level.getBlockState(adjacent);
            if (adjacentState.isAir() || adjacentState.canBeReplaced()) {
                if (CondemnedPortalShape.trySpawnPortal(level, adjacent)) {
                    spawned = true;
                    break;
                }
            }
        }
        if (!spawned) return;

        if (item == Items.FLINT_AND_STEEL) {
            stack.hurtAndBreak(1, player,
                hand == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                    : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        } else {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        event.setCanceled(true);

        // TODO: Phase 10 — replace with custom portal sound
        level.playSound(null,
                clickedPos,
                net.minecraft.sounds.SoundEvents.PORTAL_TRIGGER,
                net.minecraft.sounds.SoundSource.BLOCKS,
                0.5f, 1.0f);
    }
}

