package by.dragonsurvivalteam.dragonsurvival.magic.abilities.CaveDragon.passive;


import by.dragonsurvivalteam.dragonsurvival.DragonSurvivalMod;
import by.dragonsurvivalteam.dragonsurvival.common.dragon_types.AbstractDragonType;
import by.dragonsurvivalteam.dragonsurvival.common.dragon_types.DragonTypes;
import by.dragonsurvivalteam.dragonsurvival.config.obj.ConfigOption;
import by.dragonsurvivalteam.dragonsurvival.config.obj.ConfigSide;
import by.dragonsurvivalteam.dragonsurvival.magic.common.RegisterDragonAbility;
import by.dragonsurvivalteam.dragonsurvival.magic.common.passive.PassiveDragonAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;

@RegisterDragonAbility
public class BurnAbility extends PassiveDragonAbility{
	@ConfigOption( side = ConfigSide.SERVER, category = {"magic", "abilities", "cave_dragon", "passives"}, key = "burn", comment = "Whether the burn ability should be enabled" )
	public static Boolean burn = true;

	@Override
	public Component getDescription(){
		return Component.translatable("ds.skill.description." + getName(), getChance());
	}

	@Override
	public int getSortOrder(){
		return 4;
	}

	@Override
	public String getName(){
		return "burn";
	}

	@Override
	public AbstractDragonType getDragonType(){
		return DragonTypes.CAVE;
	}

	@Override
	public ResourceLocation[] getSkillTextures(){
		return new ResourceLocation[]{new ResourceLocation(DragonSurvivalMod.MODID, "textures/skills/cave/burn_0.png"),
		                              new ResourceLocation(DragonSurvivalMod.MODID, "textures/skills/cave/burn_1.png"),
		                              new ResourceLocation(DragonSurvivalMod.MODID, "textures/skills/cave/burn_2.png"),
		                              new ResourceLocation(DragonSurvivalMod.MODID, "textures/skills/cave/burn_3.png")};
	}


	public int getChance(){
		return 15 * getLevel();
	}

	@Override
	@OnlyIn( Dist.CLIENT )
	public ArrayList<Component> getLevelUpInfo(){
		ArrayList<Component> list = super.getLevelUpInfo();
		list.add(Component.translatable("ds.skill.chance", "+15"));
		return list;
	}

	@Override
	public int getMaxLevel(){
		return 3;
	}

	@Override
	public int getMinLevel(){
		return 0;
	}

	@Override
	public boolean isDisabled(){
		return super.isDisabled() || !burn;
	}
}