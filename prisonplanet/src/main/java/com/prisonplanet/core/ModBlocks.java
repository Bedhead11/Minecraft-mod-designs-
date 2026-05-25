package com.prisonplanet.core;

import com.prisonplanet.block.CondemnedPortalBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, "prisonplanet");

    public static final net.neoforged.neoforge.registries.DeferredHolder<Block, CondemnedPortalBlock> CONDEMNED_PORTAL =
            BLOCKS.register("condemned_portal", CondemnedPortalBlock::new);

    private ModBlocks() {}
}
