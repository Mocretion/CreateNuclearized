package com.mocretion.createnuclearized.content.kinetics;

import java.util.function.Supplier;

import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ReactorEncasedShaftBlock extends EncasedShaftBlock {

    public ReactorEncasedShaftBlock(Properties properties, Supplier<Block> casing) {
        super(properties, casing);
    }

    @Override
    public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return CNBlockEntities.REACTOR_ENCASED_SHAFT.get();
    }
}
