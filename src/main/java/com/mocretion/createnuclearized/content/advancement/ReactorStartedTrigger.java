package com.mocretion.createnuclearized.content.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class ReactorStartedTrigger extends SimpleCriterionTrigger<ReactorStartedTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        trigger(player, instance -> true);
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = EntityPredicate.ADVANCEMENT_CODEC
                .optionalFieldOf("player").xmap(TriggerInstance::new, TriggerInstance::player)
                .codec();
    }
}
