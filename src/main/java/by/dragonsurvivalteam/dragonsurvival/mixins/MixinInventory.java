package by.dragonsurvivalteam.dragonsurvival.mixins;

import by.dragonsurvivalteam.dragonsurvival.DragonSurvivalMod;
import by.dragonsurvivalteam.dragonsurvival.common.handlers.magic.ClawToolHandler;
import by.dragonsurvivalteam.dragonsurvival.config.ServerConfig;
import by.dragonsurvivalteam.dragonsurvival.util.DragonUtils;
import by.dragonsurvivalteam.dragonsurvival.util.ResourceHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin( Inventory.class )
public abstract class MixinInventory{
	@Shadow @Final public Player player;
	@Shadow @Final public NonNullList<ItemStack> items;
	@Shadow public int selected;

	@Shadow @Final public static int SLOT_OFFHAND;

	@Shadow public abstract ItemStack getItem(int pIndex);

	@Inject( at = @At( "HEAD" ), method = "getDestroySpeed", cancellable = true )
	public void getDestroySpeed(BlockState state, CallbackInfoReturnable<Float> ci){
		ItemStack mainStack = player.getInventory().getSelected();
		ItemStack breakStack = ClawToolHandler.getDragonHarvestTool(player);

		if(!ItemStack.isSame(mainStack, breakStack)){
			float tempSpeed = breakStack.getDestroySpeed(state);
			ci.setReturnValue(tempSpeed);
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	public void handleBlacklist(CallbackInfo ci) {
		if (player.tickCount % 10 != 0) {
			return;
		}

		if(player.isCreative() || player.isSpectator() || !DragonUtils.isDragon(player)) {
			return;
		}

		handleItem(selected);
		// Offhand cannot be selected
		handleItem(SLOT_OFFHAND);
	}

	private void handleItem(int slot) {
		ItemStack stack = getItem(slot);

		if((ServerConfig.blacklistedSlots.contains(slot)) && isBlacklisted(stack.getItem())){
			if(stack.isEmpty() || !stack.onDroppedByPlayer(player)){
				return;
			}

			player.drop(player.getInventory().removeItemNoUpdate(slot), false);
		}
	}

	private boolean isBlacklisted(final Item item) {
		ResourceLocation location = ResourceHelper.getKey(item);

		boolean contains = ServerConfig.blacklistedItems.contains(location.toString());

		if (contains) {
			return true;
		}

		// TODO :: Could cache the result

		for (String regex : ServerConfig.blacklistedItemsRegex) {
			String[] split = regex.split(":");

			// Just to be sure
			if (split.length != 2) {
				DragonSurvivalMod.LOGGER.warn("Regex definition for the blacklist hast the wrong format: " + regex);
				continue;
			}

			if (location.getNamespace().equals(split[0])) {
				Pattern pattern = Pattern.compile(split[1]);

				Matcher matcher = pattern.matcher(location.getPath());
				if (matcher.matches()) {
					return true;
				}
			}
		}

		return false;
	}
}