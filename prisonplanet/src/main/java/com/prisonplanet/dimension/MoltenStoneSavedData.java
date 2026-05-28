package com.prisonplanet.dimension;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Remembers solar-melted vanilla stone so liquid lava can reform into its source material.
 */
public class MoltenStoneSavedData extends SavedData {
    private static final String KEY_MOLTEN = "molten";
    private static final String KEY_POS = "pos";
    private static final String KEY_STONE = "stone";
    private final Map<Long, Integer> moltenStones = new HashMap<>();

    public static MoltenStoneSavedData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MoltenStoneSavedData::new, MoltenStoneSavedData::load, null),
                "prisonplanet_molten_stone");
    }

    public static MoltenStoneSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        MoltenStoneSavedData data = new MoltenStoneSavedData();
        ListTag molten = tag.getList(KEY_MOLTEN, Tag.TAG_COMPOUND);
        for (int i = 0; i < molten.size(); i++) {
            CompoundTag entry = molten.getCompound(i);
            data.moltenStones.put(entry.getLong(KEY_POS), entry.getInt(KEY_STONE));
        }
        return data;
    }

    public boolean melt(ServerLevel level, BlockPos pos, BlockState state) {
        int type = stoneType(state);
        if (type < 0 || moltenStones.containsKey(pos.asLong())) {
            return false;
        }
        moltenStones.put(pos.asLong(), type);
        setDirty();
        return level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
    }

    public void restoreLoaded(ServerLevel level, int maxRestores) {
        boolean changed = false;
        int restored = 0;
        Iterator<Map.Entry<Long, Integer>> entries = moltenStones.entrySet().iterator();
        while (entries.hasNext() && restored < maxRestores) {
            Map.Entry<Long, Integer> entry = entries.next();
            BlockPos pos = BlockPos.of(entry.getKey());
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (level.getBlockState(pos).is(Blocks.LAVA)) {
                level.setBlockAndUpdate(pos, stoneFor(entry.getValue()).defaultBlockState());
                clearFlowingLava(level, pos);
            }
            entries.remove();
            restored++;
            changed = true;
        }
        if (changed) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag molten = new ListTag();
        moltenStones.forEach((pos, stone) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong(KEY_POS, pos);
            entry.putInt(KEY_STONE, stone);
            molten.add(entry);
        });
        tag.put(KEY_MOLTEN, molten);
        return tag;
    }

    private static void clearFlowingLava(ServerLevel level, BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -1, -3), origin.offset(3, 1, 3))) {
            if (level.getBlockState(pos).is(Blocks.LAVA)
                    && !level.getFluidState(pos).isSource()) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static int stoneType(BlockState state) {
        if (state.is(Blocks.STONE)) return 0;
        if (state.is(Blocks.COBBLESTONE)) return 1;
        if (state.is(Blocks.GRANITE)) return 2;
        if (state.is(Blocks.DIORITE)) return 3;
        if (state.is(Blocks.ANDESITE)) return 4;
        return -1;
    }

    private static Block stoneFor(int type) {
        return switch (type) {
            case 1 -> Blocks.COBBLESTONE;
            case 2 -> Blocks.GRANITE;
            case 3 -> Blocks.DIORITE;
            case 4 -> Blocks.ANDESITE;
            default -> Blocks.STONE;
        };
    }
}
