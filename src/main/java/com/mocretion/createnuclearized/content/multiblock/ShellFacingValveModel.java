package com.mocretion.createnuclearized.content.multiblock;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.simibubi.create.foundation.model.BakedModelWrapperWithData;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public class ShellFacingValveModel extends BakedModelWrapperWithData {

    private static final ModelProperty<boolean[]> SHELL_FACES = new ModelProperty<>();

    private final BakedModel casingModel;
    private final Predicate<BlockState> isShell;

    public ShellFacingValveModel(BakedModel original, BakedModel casingModel, Predicate<BlockState> isShell) {
        super(original);
        this.casingModel = casingModel;
        this.isShell = isShell;
    }

    @Override
    protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter level, BlockPos pos,
            BlockState state, ModelData blockEntityData) {
        boolean[] shell = new boolean[Direction.values().length];
        for (Direction dir : Direction.values())
            shell[dir.get3DDataValue()] = isShell.test(level.getBlockState(pos.relative(dir)));
        return builder.with(SHELL_FACES, shell);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
            @Nullable RenderType renderType) {
        if (side == null)
            return super.getQuads(state, side, rand, data, renderType);
        boolean[] shell = data.get(SHELL_FACES);
        BakedModel model = shell != null && shell[side.get3DDataValue()] ? casingModel : originalModel;
        return model.getQuads(state, side, rand, data, renderType);
    }
}
