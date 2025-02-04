package by.dragonsurvivalteam.dragonsurvival.magic.common.active;

import by.dragonsurvivalteam.dragonsurvival.common.handlers.magic.ManaHandler;
import net.minecraft.world.entity.player.Player;

public abstract class InstantCastAbility extends ActiveDragonAbility {
	public abstract void onCast(Player player);

	@Override
	public void onKeyPressed(Player player, Runnable onFinish){
		onCast(player);
		ManaHandler.consumeMana(player, getManaCost());
		startCooldown();
		onFinish.run();
	}
}