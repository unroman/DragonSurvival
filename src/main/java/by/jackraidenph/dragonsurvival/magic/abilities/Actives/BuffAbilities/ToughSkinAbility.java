package by.jackraidenph.dragonsurvival.magic.abilities.Actives.BuffAbilities;

import by.jackraidenph.dragonsurvival.Functions;
import by.jackraidenph.dragonsurvival.handlers.Client.KeyInputHandler;
import net.minecraft.particles.ParticleType;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Locale;

public class ToughSkinAbility extends AoeBuffAbility
{
	public ToughSkinAbility(EffectInstance effect, int range, ParticleType particle, String id, String icon, int minLevel, int maxLevel, int manaCost, int castTime, int cooldown, Integer[] requiredLevels)
	{
		super(effect, range, particle, id, icon, minLevel, maxLevel, manaCost, castTime, cooldown, requiredLevels);
	}
	
	public static int getDefence(int level){
		return level * 2;
	}
	
	@Override
	public EffectInstance getEffect()
	{
		return new EffectInstance(effect.getEffect(), Functions.secondsToTicks(getDuration()) * 4, getLevel() - 1, false, false);
	}
	
	@Override
	public ToughSkinAbility createInstance()
	{
		return new ToughSkinAbility(effect, range, particle, id, icon, minLevel, maxLevel, manaCost, castTime, abilityCooldown, requiredLevels);
	}
	
	@Override
	public IFormattableTextComponent getDescription()
	{
		return new TranslationTextComponent("ds.skill.description." + getId(), getDuration(), getDefence(getLevel()));
	}
	
	@Override
	public ArrayList<ITextComponent> getInfo()
	{
		ArrayList<ITextComponent> components = super.getInfo();
		
		if(!KeyInputHandler.ABILITY3.isUnbound()) {
			components = new ArrayList<>(components.subList(0, components.size() - 1));
		}
		
		components.add(new TranslationTextComponent("ds.skill.duration.seconds", getDuration()));
		
		if(!KeyInputHandler.ABILITY3.isUnbound()) {
			String key = KeyInputHandler.ABILITY3.getKey().getDisplayName().getContents().toUpperCase(Locale.ROOT);
			
			if(key.isEmpty()){
				key = KeyInputHandler.ABILITY3.getKey().getDisplayName().getString();
			}
			components.add(new TranslationTextComponent("ds.skill.keybind", key));
		}
		
		return components;
	}
	
	public int getCastingSlowness() { return 10; }
	
	@OnlyIn( Dist.CLIENT )
	public ArrayList<ITextComponent> getLevelUpInfo(){
		ArrayList<ITextComponent> list = super.getLevelUpInfo();
		list.add(new TranslationTextComponent("ds.skill.defence", "+2"));
		return list;
	}
}
