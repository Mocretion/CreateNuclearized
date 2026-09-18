package com.mocretion.createnuclearized.content.boiler;

import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class ContainedBlazeBurnerClient {

    private ContainedBlazeBurnerClient() {}

    // The angle the blaze should look towards to watch the local player, or NaN when theres no one to watch
    // Kept here so the common block entity never references Minecraft/LocalPlayer
    static float lookAngle(BlockPos pos) {
        Player player = Minecraft.getInstance().player;
        if (player == null || player.isInvisible())
            return Float.NaN;
        double dx = player.getX() - (pos.getX() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        return AngleHelper.deg(-Mth.atan2(dz, dx)) - 90;
    }
}
