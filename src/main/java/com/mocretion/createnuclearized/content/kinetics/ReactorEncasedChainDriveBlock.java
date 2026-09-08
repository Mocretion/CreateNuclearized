package com.mocretion.createnuclearized.content.kinetics;

import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.chainDrive.ChainDriveBlock;

import net.minecraft.world.level.block.entity.BlockEntityType;

public class ReactorEncasedChainDriveBlock extends ChainDriveBlock {

    public ReactorEncasedChainDriveBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return CNBlockEntities.REACTOR_ENCASED_CHAIN_DRIVE.get();
    }
}
