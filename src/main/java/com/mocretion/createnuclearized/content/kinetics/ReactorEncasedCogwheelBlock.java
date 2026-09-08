package com.mocretion.createnuclearized.content.kinetics;

import java.util.function.Supplier;

import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ReactorEncasedCogwheelBlock extends EncasedCogwheelBlock {

    public ReactorEncasedCogwheelBlock(Properties properties, boolean large, Supplier<Block> casing) {
        super(properties, large, casing);
    }

    @Override
    public BlockEntityType<? extends SimpleKineticBlockEntity> getBlockEntityType() {
        return CNBlockEntities.REACTOR_ENCASED_COGWHEEL.get();
    }
}
