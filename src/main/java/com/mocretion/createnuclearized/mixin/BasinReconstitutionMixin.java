package com.mocretion.createnuclearized.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mocretion.createnuclearized.content.reconstitution.Reconstitution;
import com.mocretion.createnuclearized.content.reconstitution.ReconstitutionRecipe;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(BasinOperatingBlockEntity.class)
public abstract class BasinReconstitutionMixin {


    // Handles reconstitution recipes (refuel spent salts with fissile material with the exact amount needed)

    @Shadow
    protected Recipe<?> currentRecipe;

    @Shadow
    protected abstract Optional<BasinBlockEntity> getBasin();

    @Inject(method = "getMatchingRecipes", at = @At("RETURN"), cancellable = true)
    private void createnuclearized$addReconstitution(CallbackInfoReturnable<List<Recipe<?>>> cir) {
        if (!((Object) this instanceof MechanicalMixerBlockEntity)) return;
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level == null || level.isClientSide) return;
        BasinBlockEntity basin = getBasin().orElse(null);
        if (basin == null) return;

        ReconstitutionRecipe recipe = Reconstitution.find(level, basin);
        if (recipe == null) return;
        List<Recipe<?>> list = new ArrayList<>(cir.getReturnValue());
        list.add(recipe);
        cir.setReturnValue(list);
    }

    @Inject(method = "matchBasinRecipe", at = @At("HEAD"), cancellable = true)
    private void createnuclearized$matchReconstitution(Recipe<?> recipe, CallbackInfoReturnable<Boolean> cir) {
        if (!(recipe instanceof ReconstitutionRecipe reconstitution)) return;
        Optional<BasinBlockEntity> basin = getBasin();
        cir.setReturnValue(basin.isPresent() && Reconstitution.canRun(reconstitution, basin.get()));
    }

    @Inject(method = "applyBasinRecipe", at = @At("HEAD"), cancellable = true)
    private void createnuclearized$applyReconstitution(CallbackInfo ci) {
        if (!(currentRecipe instanceof ReconstitutionRecipe reconstitution)) return;
        getBasin().ifPresent(basin -> Reconstitution.apply(reconstitution, basin));
        ci.cancel();
    }
}
