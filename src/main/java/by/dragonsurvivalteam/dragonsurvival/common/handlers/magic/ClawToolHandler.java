package by.dragonsurvivalteam.dragonsurvival.common.handlers.magic;

import by.dragonsurvivalteam.dragonsurvival.DragonSurvivalMod;
import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateHandler;
import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.common.dragon_types.DragonTypes;
import by.dragonsurvivalteam.dragonsurvival.config.ServerConfig;
import by.dragonsurvivalteam.dragonsurvival.network.NetworkHandler;
import by.dragonsurvivalteam.dragonsurvival.network.claw.SyncDragonClawsMenu;
import by.dragonsurvivalteam.dragonsurvival.util.DragonLevel;
import by.dragonsurvivalteam.dragonsurvival.util.DragonUtils;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent.PickupXp;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Objects;

@EventBusSubscriber
public class ClawToolHandler{
	@SubscribeEvent
	public static void experiencePickup(PickupXp event){
		Player player = event.getEntity();

		DragonStateProvider.getCap(player).ifPresent(cap -> {
			ArrayList<ItemStack> stacks = new ArrayList<>();

			for(int i = 0; i < 4; i++){
				ItemStack clawStack = cap.getClawToolData().getClawsInventory().getItem(i);
				int mending = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, clawStack);

				if(mending > 0 && clawStack.isDamaged()){
					stacks.add(clawStack);
				}
			}

			if(stacks.size() > 0){
				ItemStack repairTime = stacks.get(player.getRandom().nextInt(stacks.size()));
				if(!repairTime.isEmpty() && repairTime.isDamaged()){

					int i = Math.min((int)(event.getOrb().value * repairTime.getXpRepairRatio()), repairTime.getDamageValue());
					event.getOrb().value -= i * 2;
					repairTime.setDamageValue(repairTime.getDamageValue() - i);
				}
			}

			event.getOrb().value = Math.max(0, event.getOrb().value);
			player.detectEquipmentUpdates();
		});
	}

	@SubscribeEvent
	public static void playerDieEvent(LivingDropsEvent event){
		// FIXME :: Add support for SoulBound etc.

		Entity ent = event.getEntity();

		if(ent instanceof Player player){
			if(!player.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) && !ServerConfig.keepClawItems){
				DragonStateHandler handler = DragonUtils.getHandler(player);

				for(int i = 0; i < handler.getClawToolData().getClawsInventory().getContainerSize(); i++){
					ItemStack stack = handler.getClawToolData().getClawsInventory().getItem(i);

					if(!stack.isEmpty()){
						if (!EnchantmentHelper.hasVanishingCurse(stack)) {
							event.getDrops().add(new ItemEntity(player.level, player.getX(), player.getY(), player.getZ(), stack));
						}

						handler.getClawToolData().getClawsInventory().setItem(i, ItemStack.EMPTY);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void dropBlocksMinedByPaw(PlayerEvent.HarvestCheck harvestCheck){
		if(!ServerConfig.bonuses || !ServerConfig.clawsAreTools){
			return;
		}
		Player playerEntity = harvestCheck.getEntity();
		DragonStateProvider.getCap(playerEntity).ifPresent(dragonStateHandler -> {
			if(dragonStateHandler.isDragon()){
				ItemStack stack = playerEntity.getMainHandItem();
				Item item = stack.getItem();
				BlockState blockState = harvestCheck.getTargetBlock();
				if(!(item instanceof DiggerItem || item instanceof SwordItem || item instanceof ShearsItem) && !harvestCheck.canHarvest()){
					harvestCheck.setCanHarvest(dragonStateHandler.canHarvestWithPaw(blockState));
				}
			}
		});
	}

	public static ItemStack getDragonHarvestTool(Player player){
		ItemStack mainStack = player.getInventory().getSelected();
		ItemStack harvestTool = mainStack;
		float newSpeed = 0F;

		DragonStateHandler cap = DragonUtils.getHandler(player);

		if(mainStack.getItem() instanceof DiggerItem || mainStack.getItem() instanceof SwordItem || mainStack.getItem() instanceof ShearsItem || mainStack.getItem() instanceof TieredItem){
			return mainStack;
		}

		Level world = player.level;
		BlockHitResult raytraceresult = Item.getPlayerPOVHitResult(world, player, ClipContext.Fluid.NONE);

		if(raytraceresult.getType() != HitResult.Type.MISS){
			BlockState state = world.getBlockState(raytraceresult.getBlockPos());

			for(int i = 1; i < 4; i++){
				ItemStack breakingItem = cap.getClawToolData().getClawsInventory().getItem(i);

				if(!breakingItem.isEmpty() && breakingItem.isCorrectToolForDrops(state)){
					float tempSpeed = breakingItem.getDestroySpeed(state);

					if(breakingItem.getItem() instanceof DiggerItem item){
						tempSpeed = item.getDestroySpeed(breakingItem, state);
					}

					if(tempSpeed > newSpeed){
						newSpeed = tempSpeed;
						harvestTool = breakingItem;
					}
				}
			}
		}

		return harvestTool;
	}

	/**
	 *
	 * @return Only the sword in the dragon tool slot <br>
	 * If the main hand has a {@link TieredItem} the returned result will be {@link ItemStack#EMPTY}
	 */
	public static ItemStack getDragonSword(final LivingEntity player) {
		if (!(player instanceof Player)) {
			return ItemStack.EMPTY;
		}

		ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

		if (itemInHand.getItem() instanceof TieredItem) {
			return ItemStack.EMPTY;
		}

		DragonStateHandler cap = DragonUtils.getHandler(player);

		return cap.getClawToolData().getClawsInventory().getItem(0);
	}

	/** Handle tool breaking for the dragon */
	@SubscribeEvent
	public static void onToolBreak(final PlayerDestroyItemEvent event) {
		if (event.getHand() == null) return;
		Player player = event.getEntity();

		if (DragonUtils.isDragon(player)) {
			ItemStack clawTool = getDragonHarvestTool(player);

			if (ItemStack.matches(clawTool, event.getOriginal())) {
				player.broadcastBreakEvent(event.getHand());
			} else {
				if (!player.level.isClientSide) {
					DragonStateHandler handler = DragonUtils.getHandler(player);
					NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new SyncDragonClawsMenu(player.getId(), handler.getClawToolData().isClawsMenuOpen(), handler.getClawToolData().getClawsInventory()));
				}
			}
		}
	}

	@Mod.EventBusSubscriber(modid = DragonSurvivalMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class Event_busHandler {
		@SubscribeEvent
		public void modifyBreakSpeed(final PlayerEvent.BreakSpeed event) {
			if (!ServerConfig.bonuses || !ServerConfig.clawsAreTools) {
				return;
			}

			Player player = event.getEntity();
			ItemStack mainStack = player.getMainHandItem();
			DragonStateHandler handler = DragonUtils.getHandler(player);

			if (mainStack.getItem() instanceof TieredItem || mainStack.getItem() instanceof SwordItem || mainStack.getItem() instanceof ShearsItem || !handler.isDragon()) {
				// Bonus is not applied to items in the hotbar
				return;
			}

			BlockState blockState = event.getState();
			float originalSpeed = event.getOriginalSpeed();

			float bonus = 1F;
			float unlockedBonus = 1F;

			/* TODO ::
			Setting the the bonus or base harvest level to diamond doesn't improve the speed here
			Should the speed bonus scale with the bonus harvest level or stay at these static values?
			*/

			if (handler.getLevel() == DragonLevel.NEWBORN && ServerConfig.bonusUnlockedAt == DragonLevel.NEWBORN) {
				unlockedBonus = 2F;
			} else if (handler.getLevel() == DragonLevel.YOUNG && ServerConfig.bonusUnlockedAt != DragonLevel.ADULT) {
				unlockedBonus = 2F;
			} else if (handler.getLevel() == DragonLevel.ADULT) {
				unlockedBonus = 4F;
				bonus = 2F;
			}

			for (TagKey<Block> tagKey : handler.getType().mineableBlocks()) {
				if (blockState.is(tagKey)) {
					bonus = unlockedBonus;

					break;
				}
			}

			event.setNewSpeed(originalSpeed * bonus);
		}
	}
}