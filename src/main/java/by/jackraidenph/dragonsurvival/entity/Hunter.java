package by.jackraidenph.dragonsurvival.entity;

import by.jackraidenph.dragonsurvival.Functions;
import by.jackraidenph.dragonsurvival.config.ConfigHandler;
import by.jackraidenph.dragonsurvival.gecko.Knight;
import by.jackraidenph.dragonsurvival.goals.FollowMobGoal;
import by.jackraidenph.dragonsurvival.handlers.DragonEffects;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.monster.AbstractIllagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effects;
import net.minecraft.world.World;

public abstract class Hunter extends CreatureEntity implements DragonHunter {
    public Hunter(EntityType<? extends CreatureEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new SwimGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomWalkingGoal(this, 1));
        goalSelector.addGoal(7, new FollowMobGoal<>(Knight.class, this, 15));

        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, 0, true, true, livingEntity ->
                (livingEntity.hasEffect(Effects.BAD_OMEN) || livingEntity.hasEffect(DragonEffects.EVIL_DRAGON))));
        targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, MonsterEntity.class, 0, true, true, livingEntity ->
                (livingEntity instanceof net.minecraft.entity.monster.IMob && !(livingEntity instanceof DragonHunter))));
        targetSelector.addGoal(7, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return tickCount >= Functions.minutesToTicks(ConfigHandler.COMMON.hunterDespawnDelay.get());
    }

    public AbstractIllagerEntity.ArmPose getArmPose() {
        return AbstractIllagerEntity.ArmPose.ATTACKING;
    }
}
