package com.mocretion.createnuclearized.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mocretion.createnuclearized.content.filter.SaltStatsAttribute;
import com.simibubi.create.content.logistics.filter.AttributeFilterWhitelistMode;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;

import net.createmod.catnip.data.Pair;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

@Mixin(targets = "com.simibubi.create.content.logistics.filter.FilterItemStack$AttributeFilterItemStack")
public abstract class AttributeFilterFluidMixin {

    @Shadow public List<Pair<ItemAttribute, Boolean>> attributeTests;
    @Shadow public AttributeFilterWhitelistMode whitelistMode;

    @Inject(method = "test(Lnet/minecraft/world/level/Level;Lnet/neoforged/neoforge/fluids/FluidStack;Z)Z",
            at = @At("HEAD"), cancellable = true)
    private void createnuclearized$testFluid(Level world, FluidStack stack, boolean matchNBT,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (attributeTests.isEmpty() || stack.isEmpty()) return;
        for (Pair<ItemAttribute, Boolean> entry : attributeTests) {
            ItemAttribute attribute = entry.getFirst();
            boolean inverted = entry.getSecond();
            boolean applies = attribute instanceof SaltStatsAttribute salt && salt.appliesTo(stack);
            boolean matches = applies != inverted;
            if (matches) {
                switch (whitelistMode) {
                    case BLACKLIST -> { cir.setReturnValue(false); return; }
                    case WHITELIST_CONJ -> { continue; }
                    case WHITELIST_DISJ -> { cir.setReturnValue(true); return; }
                }
            } else {
                switch (whitelistMode) {
                    case BLACKLIST, WHITELIST_DISJ -> { continue; }
                    case WHITELIST_CONJ -> { cir.setReturnValue(false); return; }
                }
            }
        }
        cir.setReturnValue(switch (whitelistMode) {
            case BLACKLIST, WHITELIST_CONJ -> true;
            case WHITELIST_DISJ -> false;
        });
    }
}
