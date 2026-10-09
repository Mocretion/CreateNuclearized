package com.mocretion.createnuclearized.content.boiler;

import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ContainedBlazeBurnerBlock extends Block implements IBE<ContainedBlazeBurnerBlockEntity> {

    public ContainedBlazeBurnerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<ContainedBlazeBurnerBlockEntity> getBlockEntityClass() {
        return ContainedBlazeBurnerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ContainedBlazeBurnerBlockEntity> getBlockEntityType() {
        return CNBlockEntities.CONTAINED_BLAZE_BURNER.get();
    }
}
