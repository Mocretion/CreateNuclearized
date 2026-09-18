package com.mocretion.createnuclearized.content.turbine;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.platform.CatnipServices;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TurbineRotorBlockItem extends BlockItem {

    public TurbineRotorBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (result != InteractionResult.FAIL)
            return result;
        if (context.getLevel().isClientSide())
            CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> showBounds(context));
        return result;
    }

    @OnlyIn(Dist.CLIENT)
    private void showBounds(BlockPlaceContext context) {
        if (!(context.getPlayer() instanceof LocalPlayer player))
            return;
        if (!(getBlock() instanceof TurbineRotorBlock rotor))
            return;
        int hb = rotor.hitboxRadius();
        if (hb <= 0)
            return;
        BlockPos pos = context.getClickedPos();
        Axis axis = rotor.getAxisForPlacement(context);
        double ix = axis == Axis.X ? 0 : hb;
        double iy = axis == Axis.Y ? 0 : hb;
        double iz = axis == Axis.Z ? 0 : hb;
        Outliner.getInstance()
                .showAABB(Pair.of("turbine_rotor", pos), new AABB(pos).inflate(ix, iy, iz))
                .colored(0xFF_ff5d6c);
        player.displayClientMessage(Component.translatable("createnuclearized.turbine_rotor.no_space")
                .withStyle(s -> s.withColor(0xff5d6c)), true);
    }
}
