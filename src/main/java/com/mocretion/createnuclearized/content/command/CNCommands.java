package com.mocretion.createnuclearized.content.command;

import java.util.HashMap;
import java.util.Map;

import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class CNCommands {

    private CNCommands() {}

    private static final SuggestionProvider<CommandSourceStack> SPENT_FLUID_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(ReactorFuel.values().stream()
                    .map(ReactorFuel::outputFluid)
                    .map(BuiltInRegistries.FLUID::getKey)
                    .filter(id -> id != null)
                    .map(ResourceLocation::toString), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("createnuclearized")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.literal("spentbucket")
                        .then(Commands.argument("fluid", ResourceLocationArgument.id())
                                .suggests(SPENT_FLUID_SUGGESTIONS)
                                .then(Commands.argument("heat", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("unconsumed", IntegerArgumentType.integer(0, 100))
                                                .then(Commands.argument("bred", IntegerArgumentType.integer(0, 100))
                                                        .executes(ctx -> run(ctx, -1))
                                                        .then(Commands.argument("off_gas", IntegerArgumentType.integer(0, 100))
                                                                .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, "off_gas"))))))))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, int offGasArg) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "fluid");
        int heat = IntegerArgumentType.getInteger(ctx, "heat");
        int unconsumed = IntegerArgumentType.getInteger(ctx, "unconsumed");
        int bred = IntegerArgumentType.getInteger(ctx, "bred");

        Fluid fluid = BuiltInRegistries.FLUID.getOptional(id).orElse(Fluids.EMPTY);
        ReactorFuel fuel = ReactorFuel.forOutput(fluid);
        if (fuel == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown spent fluid: " + id));
            return 0;
        }

        int offGas;
        if (offGasArg < 0) {
            int remainder = 100 - unconsumed - bred;
            if (remainder < 0) {
                ctx.getSource().sendFailure(Component.literal("unconsumed + bred exceeds 100%"));
                return 0;
            }
            offGas = remainder;
        } else {
            offGas = offGasArg;
        }

        Map<ResourceLocation, Integer> elements = new HashMap<>();
        if (fuel.unconsumed() != null && unconsumed > 0)
            elements.merge(fuel.unconsumed().id(), unconsumed, Integer::sum);
        if (fuel.bred() != null && bred > 0)
            elements.merge(fuel.bred().id(), bred, Integer::sum);

        SaltStats stats = new SaltStats(heat, elements, offGas);

        ItemStack bucket = new ItemStack(fluid.getBucket());
        bucket.set(CNDataComponents.SALT_STATS.get(), stats);

        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!player.getInventory().add(bucket))
            player.drop(bucket, false);

        int finalOffGas = offGas;
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Gave " + id + " bucket (heat " + heat + ", unconsumed " + unconsumed
                        + "%, bred " + bred + "%, off-gas " + finalOffGas + "%)"), true);
        return 1;
    }
}
