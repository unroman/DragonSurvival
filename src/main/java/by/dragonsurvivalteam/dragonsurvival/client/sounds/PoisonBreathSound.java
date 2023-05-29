package by.dragonsurvivalteam.dragonsurvival.client.sounds;


import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.magic.abilities.ForestDragon.active.ForestBreathAbility;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn( Dist.CLIENT )
public class PoisonBreathSound extends AbstractTickableSoundInstance{
	private final ForestBreathAbility ability;

	public PoisonBreathSound(ForestBreathAbility ability){
		super(SoundRegistry.forestBreathLoop, SoundSource.PLAYERS);

		looping = true;
		this.x = ability.getPlayer().getX();
		this.y = ability.getPlayer().getY();
		this.z = ability.getPlayer().getZ();

		this.ability = ability;
	}

	@Override
	public void tick(){
		if(ability.getPlayer() == null || ability.chargeTime == 0)
			stop();

		this.x = ability.getPlayer().getX();
		this.y = ability.getPlayer().getY();
		this.z = ability.getPlayer().getZ();
	}

	@Override
	public boolean canStartSilent(){
		return true;
	}
}