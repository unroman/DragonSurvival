package by.jackraidenph.dragonsurvival.magic.abilities.Passives;

import by.jackraidenph.dragonsurvival.magic.common.PassiveDragonAbility;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;

public class BurnAbility extends PassiveDragonAbility
{
	public BurnAbility(String abilityId, String icon, int minLevel, int maxLevel)
	{
		super(abilityId, icon, minLevel, maxLevel);
	}
	
	public int getChance(){
		return 15 * getLevel();
	}
	
	@Override
	public BurnAbility createInstance()
	{
		return new BurnAbility(id, icon, minLevel, maxLevel);
	}
	
	@Override
	public IFormattableTextComponent getDescription()
	{
		return new TranslationTextComponent("ds.skill.description." + getId(), getChance());
	}
	
	@OnlyIn( Dist.CLIENT )
	public ArrayList<ITextComponent> getLevelUpInfo(){
		ArrayList<ITextComponent> list = super.getLevelUpInfo();
		list.add(new TranslationTextComponent("ds.skill.chance", "+15"));
		return list;
	}
}
