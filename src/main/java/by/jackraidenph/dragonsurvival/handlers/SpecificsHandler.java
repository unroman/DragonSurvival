package by.jackraidenph.dragonsurvival.handlers;

import by.jackraidenph.dragonsurvival.DragonSurvivalMod;
import by.jackraidenph.dragonsurvival.Functions;
import by.jackraidenph.dragonsurvival.capability.DragonStateHandler;
import by.jackraidenph.dragonsurvival.capability.DragonStateProvider;
import by.jackraidenph.dragonsurvival.config.ConfigHandler;
import by.jackraidenph.dragonsurvival.handlers.Server.NetworkHandler;
import by.jackraidenph.dragonsurvival.magic.DragonAbilities;
import by.jackraidenph.dragonsurvival.magic.abilities.Passives.CliffhangerAbility;
import by.jackraidenph.dragonsurvival.magic.abilities.Passives.ContrastShowerAbility;
import by.jackraidenph.dragonsurvival.magic.abilities.Passives.LightInDarknessAbility;
import by.jackraidenph.dragonsurvival.magic.abilities.Passives.WaterAbility;
import by.jackraidenph.dragonsurvival.magic.common.DragonAbility;
import by.jackraidenph.dragonsurvival.network.StartJump;
import by.jackraidenph.dragonsurvival.network.SyncCapabilityDebuff;
import by.jackraidenph.dragonsurvival.registration.DragonEffects;
import by.jackraidenph.dragonsurvival.renderer.magic.CaveLavaFluidRenderer;
import by.jackraidenph.dragonsurvival.util.DamageSources;
import by.jackraidenph.dragonsurvival.util.DragonLevel;
import by.jackraidenph.dragonsurvival.util.DragonType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.FluidBlockRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.PotionEntity;
import net.minecraft.entity.projectile.SnowballEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = DragonSurvivalMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SpecificsHandler {
	
	public static final ResourceLocation DRAGON_HUD = new ResourceLocation(DragonSurvivalMod.MODID + ":textures/gui/dragon_hud.png");

	public static Map<DragonType, List<Block>> DRAGON_SPEEDUP_BLOCKS;
	public static List<Block> SEA_DRAGON_HYDRATION_BLOCKS;
	public static List<Item> SEA_DRAGON_HYDRATION_USE_ALTERNATIVES;

	@SubscribeEvent
	public static void onConfigLoad(ModConfig.Loading event) {
		if (event.getConfig().getType() == Type.SERVER) {
			rebuildSpeedupBlocksMap();
			rebuildSeaHydrationLists();
		}
	}

	@SubscribeEvent
	public static void onConfigReload(ModConfig.Reloading event) {
		if (event.getConfig().getType() == Type.SERVER) {
			rebuildSpeedupBlocksMap();
			rebuildSeaHydrationLists();
		}
	}
	
	private static void rebuildSpeedupBlocksMap() {
		HashMap<DragonType, List<Block>> speedupMap = new HashMap<>();
		speedupMap.put(DragonType.CAVE, buildDragonSpeedupMap(DragonType.CAVE));
		speedupMap.put(DragonType.FOREST, buildDragonSpeedupMap(DragonType.FOREST));
		speedupMap.put(DragonType.SEA, buildDragonSpeedupMap(DragonType.SEA));
		DRAGON_SPEEDUP_BLOCKS = speedupMap;
	}
	
	private static List<Block> buildDragonSpeedupMap(DragonType type) {
		ArrayList<Block> speedupMap = new ArrayList<>();
		String[] configSpeedups;
		switch (type) {
			case CAVE:
				configSpeedups = ConfigHandler.SERVER.caveSpeedupBlocks.get().toArray(new String[0]);
				break;
			case FOREST:
				configSpeedups = ConfigHandler.SERVER.forestSpeedupBlocks.get().toArray(new String[0]);
				break;
			case SEA:
				configSpeedups = ConfigHandler.SERVER.seaSpeedupBlocks.get().toArray(new String[0]);
				break;
			default:
				configSpeedups = new String[0];
				break;
		}
		for (String entry : configSpeedups) {
			final String[] sEntry = entry.split(":");
			final ResourceLocation rlEntry = new ResourceLocation(sEntry[1], sEntry[2]);
			if (sEntry[0].equalsIgnoreCase("tag")) {
				final ITag<Block> tag = BlockTags.getAllTags().getTag(rlEntry);
				if (tag != null && tag.getValues().size() != 0)
					speedupMap.addAll(tag.getValues());
				else
					DragonSurvivalMod.LOGGER.warn("Null or empty tag '{}:{}' in {} dragon speedup block config.", sEntry[1], sEntry[2], type.toString().toLowerCase());
			} else {
				final Block block = ForgeRegistries.BLOCKS.getValue(rlEntry);
				if (block != Blocks.AIR)
					speedupMap.add(block);
				else
					DragonSurvivalMod.LOGGER.warn("Unknown block '{}:{}' in {} dragon speedup block config.", sEntry[1], sEntry[2], type.toString().toLowerCase());
			}
		}
		return speedupMap;
	}
	
	private static void rebuildSeaHydrationLists() {
		ArrayList<Block> hydrationBlocks = new ArrayList<>();
		String[] configHydrationBlocks = ConfigHandler.SERVER.seaHydrationBlocks.get().toArray(new String[0]);
		for (String entry : configHydrationBlocks) {
			final String[] sEntry = entry.split(":");
			final ResourceLocation rlEntry = new ResourceLocation(sEntry[1], sEntry[2]);
			if (sEntry[0].equalsIgnoreCase("tag")) {
				final ITag<Block> tag = BlockTags.getAllTags().getTag(rlEntry);
				if (tag != null && tag.getValues().size() != 0)
					hydrationBlocks.addAll(tag.getValues());
				else
					DragonSurvivalMod.LOGGER.warn("Null or empty tag '{}:{}' in sea dragon hydraton block config.", sEntry[1], sEntry[2]);
			} else {
				final Block block = ForgeRegistries.BLOCKS.getValue(rlEntry);
				if (block != Blocks.AIR)
					hydrationBlocks.add(block);
				else
					DragonSurvivalMod.LOGGER.warn("Unknown block '{}:{}' in sea dragon hydration block config.", sEntry[1], sEntry[2]);
			}
		}
		SEA_DRAGON_HYDRATION_BLOCKS = hydrationBlocks;
		ArrayList<Item> hydrationItems = new ArrayList<>();
		String[] configHydrationItems = ConfigHandler.SERVER.seaAdditionalWaterUseables.get().toArray(new String[0]);
		for (String entry : configHydrationItems) {
			final String[] sEntry = entry.split(":");
			final ResourceLocation rlEntry = new ResourceLocation(sEntry[1], sEntry[2]);
			if (sEntry[0].equalsIgnoreCase("tag")) {
				final ITag<Item> tag = ItemTags.getAllTags().getTag(rlEntry);
				if (tag != null && tag.getValues().size() != 0)
					hydrationItems.addAll(tag.getValues());
				else
					DragonSurvivalMod.LOGGER.warn("Null or empty tag '{}:{}' in sea dragon hydration block config.", sEntry[1], sEntry[2]);
			} else {
				final Item item = ForgeRegistries.ITEMS.getValue(rlEntry);
				if (item != Items.AIR)
					hydrationItems.add(item);
				else
					DragonSurvivalMod.LOGGER.warn("Unknown block '{}:{}' in sea dragon hydration block config.", sEntry[1], sEntry[2]);
			}
		}
		SEA_DRAGON_HYDRATION_USE_ALTERNATIVES = hydrationItems;
	}
	
	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
    public void onRenderOverlayPreTick(RenderGameOverlayEvent.Pre event) {
    	Minecraft mc = Minecraft.getInstance();
    	ClientPlayerEntity player = mc.player;
    	DragonStateProvider.getCap(player).ifPresent(playerStateHandler -> {
    		if (event.getType() == RenderGameOverlayEvent.ElementType.AIR) {
    			if (playerStateHandler.getType() == DragonType.SEA && ConfigHandler.SERVER.bonuses.get() && ConfigHandler.SERVER.seaSwimmingBonuses.get())
    				event.setCanceled(true);
    			if (playerStateHandler.getDebuffData().timeWithoutWater > 0 && playerStateHandler.getType() == DragonType.SEA && ConfigHandler.SERVER.penalties.get() && ConfigHandler.SERVER.seaTicksWithoutWater.get() != 0) {
    				RenderSystem.enableBlend();
        			mc.getTextureManager().bind(DRAGON_HUD);
        			
        			final int right_height = ForgeIngameGui.right_height;
    				ForgeIngameGui.right_height += 10;

    				int maxTimeWithoutWater = ConfigHandler.SERVER.seaTicksWithoutWater.get();
				    DragonAbility waterAbility = playerStateHandler.getAbility(DragonAbilities.WATER);
				
				    if(waterAbility != null){
					    maxTimeWithoutWater +=  Functions.secondsToTicks(((WaterAbility)waterAbility).getDuration());
				    }
					
				    double timeWithoutWater = maxTimeWithoutWater - playerStateHandler.getDebuffData().timeWithoutWater;
    				boolean flag = false;
    				if (timeWithoutWater < 0) {
    					flag = true;
    					timeWithoutWater = Math.abs(timeWithoutWater);
    				}
    				
                    final int left = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 + 91;
                    final int top = Minecraft.getInstance().getWindow().getGuiScaledHeight() - right_height;
                    final int full = flag ? MathHelper.floor((double) timeWithoutWater * 10.0D / maxTimeWithoutWater) : MathHelper.ceil((double) (timeWithoutWater - 2) * 10.0D / maxTimeWithoutWater);
                    final int partial = MathHelper.ceil((double) timeWithoutWater * 10.0D / maxTimeWithoutWater) - full;

                    for (int i = 0; i < full + partial; ++i)
                    	Minecraft.getInstance().gui.blit(event.getMatrixStack(), left - i * 8 - 9, top, (flag ? 18 : i < full ? 0 : 9), 36, 9, 9);
                    	
                    
                    mc.getTextureManager().bind(AbstractGui.GUI_ICONS_LOCATION);
                    RenderSystem.disableBlend();
    			}
    			if (playerStateHandler.getLavaAirSupply() < ConfigHandler.SERVER.caveLavaSwimmingTicks.get() && playerStateHandler.getType() == DragonType.CAVE && ConfigHandler.SERVER.bonuses.get() && ConfigHandler.SERVER.caveLavaSwimmingTicks.get() != 0 && ConfigHandler.SERVER.caveLavaSwimming.get()) {
    				RenderSystem.enableBlend();
        			mc.getTextureManager().bind(DRAGON_HUD);
        			
        			final int right_height = ForgeIngameGui.right_height;
    				ForgeIngameGui.right_height += 10;

                    final int left = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 + 91;
                    final int top = Minecraft.getInstance().getWindow().getGuiScaledHeight() - right_height;
                    final int full = MathHelper.ceil((double) (playerStateHandler.getLavaAirSupply() - 2) * 10.0D / ConfigHandler.SERVER.caveLavaSwimmingTicks.get());
                    final int partial = MathHelper.ceil((double) playerStateHandler.getLavaAirSupply() * 10.0D / ConfigHandler.SERVER.caveLavaSwimmingTicks.get()) - full;

                    for (int i = 0; i < full + partial; ++i)
                    	Minecraft.getInstance().gui.blit(event.getMatrixStack(), left - i * 8 - 9, top, (i < full ? 0 : 9), 27, 9, 9);
                    
                    mc.getTextureManager().bind(AbstractGui.GUI_ICONS_LOCATION);
                    RenderSystem.disableBlend();
    			}
    			if (playerStateHandler.getDebuffData().timeInDarkness > 0 && playerStateHandler.getType() == DragonType.FOREST && ConfigHandler.SERVER.penalties.get() && ConfigHandler.SERVER.forestStressTicks.get() != 0 && !player.hasEffect(DragonEffects.STRESS)) {
    				RenderSystem.enableBlend();
        			mc.getTextureManager().bind(DRAGON_HUD);
        			
        			final int right_height = ForgeIngameGui.right_height;
    				ForgeIngameGui.right_height += 10;

					int maxTimeInDarkness = ConfigHandler.SERVER.forestStressTicks.get();
				    DragonAbility lightInDarkness = playerStateHandler.getAbility(DragonAbilities.LIGHT_IN_DARKNESS);
				
				    if(lightInDarkness != null){
					    maxTimeInDarkness +=  Functions.secondsToTicks(((LightInDarknessAbility)lightInDarkness).getDuration());
				    }
					
    				final int timeInDarkness = maxTimeInDarkness - Math.min(playerStateHandler.getDebuffData().timeInDarkness, maxTimeInDarkness);
    				
                    final int left = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 + 91;
                    final int top = Minecraft.getInstance().getWindow().getGuiScaledHeight() - right_height;
                    final int full = MathHelper.ceil((double) (timeInDarkness - 2) * 10.0D / maxTimeInDarkness);
                    final int partial = MathHelper.ceil((double) timeInDarkness * 10.0D / maxTimeInDarkness) - full;

                    for (int i = 0; i < full + partial; ++i)
                    	Minecraft.getInstance().gui.blit(event.getMatrixStack(), left - i * 8 - 9, top, (i < full ? 0 : 9), 45, 9, 9);
                    
                    mc.getTextureManager().bind(AbstractGui.GUI_ICONS_LOCATION);
                    RenderSystem.disableBlend();
    			}
			
			    if (playerStateHandler.getDebuffData().timeInRain > 0 && playerStateHandler.getType() == DragonType.CAVE && ConfigHandler.SERVER.penalties.get() && ConfigHandler.SERVER.caveRainDamage.get() != 0.0) {
				    RenderSystem.enableBlend();
				    mc.getTextureManager().bind(DRAGON_HUD);
				
				    final int right_height = ForgeIngameGui.right_height;
				    ForgeIngameGui.right_height += 10;
				
				    DragonAbility contrastShower = playerStateHandler.getAbility(DragonAbilities.CONTRAST_SHOWER);
				    int maxRainTime = 0;
				
				    if(contrastShower != null){
					    maxRainTime +=  Functions.secondsToTicks(((ContrastShowerAbility)contrastShower).getDuration());
				    }
				
				
				    final int timeInRain = maxRainTime - Math.min(playerStateHandler.getDebuffData().timeInRain, maxRainTime);
				
				    final int left = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 + 91;
				    final int top = Minecraft.getInstance().getWindow().getGuiScaledHeight() - right_height;
				    final int full = MathHelper.ceil((double) (timeInRain - 2) * 10.0D / maxRainTime);
				    final int partial = MathHelper.ceil((double) timeInRain * 10.0D / maxRainTime) - full;
				
				    for (int i = 0; i < full + partial; ++i)
					    Minecraft.getInstance().gui.blit(event.getMatrixStack(), left - i * 8 - 9, top, (i < full ? 0 : 9), 54, 9, 9);
				
				    mc.getTextureManager().bind(AbstractGui.GUI_ICONS_LOCATION);
				    RenderSystem.disableBlend();
			    }
    		}
    	});
    }
    
    private static boolean wasCaveDragon = false;
    private static FluidBlockRenderer prevFluidRenderer;
    
    @SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public void onRenderWorldLastEvent(RenderWorldLastEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientPlayerEntity player = minecraft.player;
		DragonStateProvider.getCap(player).ifPresent(playerStateHandler -> {
			if (playerStateHandler.getType() == DragonType.CAVE && ConfigHandler.SERVER.bonuses.get() && ConfigHandler.SERVER.caveLavaSwimming.get()) {
				if (!wasCaveDragon) {
					if(player.hasEffect(DragonEffects.LAVA_VISION)) {
						RenderType lavaType = RenderType.translucent();
						RenderTypeLookup.setRenderLayer(Fluids.LAVA, lavaType);
						RenderTypeLookup.setRenderLayer(Fluids.FLOWING_LAVA, lavaType);
						prevFluidRenderer = minecraft.getBlockRenderer().liquidBlockRenderer;
						minecraft.getBlockRenderer().liquidBlockRenderer = new CaveLavaFluidRenderer();
						minecraft.levelRenderer.allChanged();
					}
				}else{
					if(!player.hasEffect(DragonEffects.LAVA_VISION)){
						if (prevFluidRenderer != null) {
							RenderType lavaType = RenderType.solid();
							RenderTypeLookup.setRenderLayer(Fluids.LAVA, lavaType);
							RenderTypeLookup.setRenderLayer(Fluids.FLOWING_LAVA, lavaType);
							minecraft.getBlockRenderer().liquidBlockRenderer = prevFluidRenderer;
						}
						minecraft.levelRenderer.allChanged();
					}
				}
    		}else {
    			if (wasCaveDragon) {
    				if (prevFluidRenderer != null) {
    					RenderType lavaType = RenderType.solid();
                        RenderTypeLookup.setRenderLayer(Fluids.LAVA, lavaType);
						RenderTypeLookup.setRenderLayer(Fluids.FLOWING_LAVA, lavaType);
						minecraft.getBlockRenderer().liquidBlockRenderer = prevFluidRenderer;
    				}
					minecraft.levelRenderer.allChanged();
    			}
    		}
    		wasCaveDragon = playerStateHandler.getType() == DragonType.CAVE && player.hasEffect(DragonEffects.LAVA_VISION);
    	});
    }
    
    @SubscribeEvent
    public void onDamage(LivingAttackEvent event) {
        LivingEntity livingEntity = event.getEntityLiving();
        DamageSource damageSource = event.getSource();
        DragonStateProvider.getCap(livingEntity).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon()) {
				if(ConfigHandler.SERVER.bonuses.get()){
					if (dragonStateHandler.getType() == DragonType.CAVE && ConfigHandler.SERVER.caveFireImmunity.get()){
						if(damageSource.isFire() && ConfigHandler.SERVER.caveFireImmunity.get()){
							event.setCanceled(true);
						}
						
					}else if(dragonStateHandler.getType() == DragonType.FOREST){
						if(damageSource == DamageSource.SWEET_BERRY_BUSH && ConfigHandler.SERVER.forestBushImmunity.get()){
							event.setCanceled(true);
						}else if(damageSource == DamageSource.CACTUS && ConfigHandler.SERVER.forestCactiImmunity.get()){
							event.setCanceled(true);
						}
					}
				}

				
				if(ConfigHandler.SERVER.caveSplashDamage.get() != 0.0) {
					if (dragonStateHandler.getType() == DragonType.CAVE && !livingEntity.hasEffect(DragonEffects.FIRE)) {
						if (damageSource instanceof IndirectEntityDamageSource) {
							if (damageSource.getDirectEntity() instanceof SnowballEntity) {
								livingEntity.hurt(DamageSource.GENERIC, ConfigHandler.SERVER.caveSplashDamage.get().floatValue());
							}
						}
					}
				}
			}
        });
    }
    
    @SubscribeEvent
    public void removeLavaFootsteps(PlaySoundAtEntityEvent event) {
    	if (!(event.getEntity() instanceof PlayerEntity))
    		return;
    	PlayerEntity player = (PlayerEntity)event.getEntity();
    	DragonStateProvider.getCap(player).ifPresent(dragonStateHandler -> {
    		if (dragonStateHandler.getType() == DragonType.CAVE && ConfigHandler.SERVER.bonuses.get() && ConfigHandler.SERVER.caveLavaSwimming.get() && DragonSizeHandler.getOverridePose(player) == Pose.SWIMMING && event.getSound().getRegistryName().getPath().contains(".step"))
    			event.setCanceled(true);
    	});
    }
	
	
	
	
    @SubscribeEvent
    public void modifyBreakSpeed(PlayerEvent.BreakSpeed breakSpeedEvent) {
    	if (!ConfigHandler.SERVER.bonuses.get() || !ConfigHandler.SERVER.clawsAreTools.get())
    		return;
        PlayerEntity playerEntity = breakSpeedEvent.getPlayer();
		
        DragonStateProvider.getCap(playerEntity).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon()) {
                ItemStack mainStack = playerEntity.getMainHandItem();
                BlockState blockState = breakSpeedEvent.getState();
                Item item = mainStack.getItem();
				
				float speed = breakSpeedEvent.getOriginalSpeed();
				float newSpeed = 0F;
				
				for(int i = 1; i < 4; i++){
					if(blockState.getHarvestTool() == DragonStateHandler.CLAW_TOOL_TYPES[i]){
						ItemStack breakingItem = dragonStateHandler.clawsInventory.getItem(i);
						if(!breakingItem.isEmpty()){
							float tempSpeed = breakingItem.getDestroySpeed(blockState) * 0.7F;
							
							if(tempSpeed > newSpeed){
								newSpeed = tempSpeed;
							}
						}
					}
				}
				
				if(!playerEntity.getMainHandItem().isEmpty()){
					float tempSpeed = playerEntity.getMainHandItem().getDestroySpeed(blockState);
					
					if(tempSpeed > newSpeed){
						newSpeed = 0;
					}
				}
				
				
                if (!(item instanceof ToolItem || item instanceof SwordItem || item instanceof ShearsItem)) {
                    switch (dragonStateHandler.getLevel()) {
                        case BABY:
                        	if (ConfigHandler.SERVER.bonusUnlockedAt.get() != DragonLevel.BABY) {
                        		breakSpeedEvent.setNewSpeed((speed * 2.0F) + newSpeed);
                        		break;
                        	}
                        case YOUNG:
                        	if (ConfigHandler.SERVER.bonusUnlockedAt.get() == DragonLevel.ADULT && dragonStateHandler.getLevel() != DragonLevel.BABY) {
                        		breakSpeedEvent.setNewSpeed((speed * 2.0F) + newSpeed);
                        		break;
                        	}
                        case ADULT:
                            switch (dragonStateHandler.getType()) {
                                case FOREST:
                                    if (blockState.getHarvestTool() == ToolType.AXE) {
                                        breakSpeedEvent.setNewSpeed((speed * 4.0F) + newSpeed);
                                    } else breakSpeedEvent.setNewSpeed((speed * 2.0F) + newSpeed);
                                    break;
                                case CAVE:
                                    if (blockState.getHarvestTool() == ToolType.PICKAXE) {
                                        breakSpeedEvent.setNewSpeed((speed * 4.0F) + newSpeed);
                                    } else breakSpeedEvent.setNewSpeed((speed * 2.0F) + newSpeed);
                                    break;
                                case SEA:
                                    if (blockState.getHarvestTool() == ToolType.SHOVEL) {
                                        breakSpeedEvent.setNewSpeed((speed * 4.0F) + newSpeed);
                                    } else breakSpeedEvent.setNewSpeed((speed * 2.0F) + newSpeed);
                                    if (playerEntity.isInWaterOrBubble()) {
                                        breakSpeedEvent.setNewSpeed((speed * 1.4f) + newSpeed);
                                    }
                                    break;
                            }
                            break;
                    }
                } else {
                    breakSpeedEvent.setNewSpeed((speed * 0.7f) + newSpeed);
                }
            }
        });
    }
    
    @SubscribeEvent
    public void dropBlocksMinedByPaw(PlayerEvent.HarvestCheck harvestCheck) {
    	if (!ConfigHandler.SERVER.bonuses.get() || !ConfigHandler.SERVER.clawsAreTools.get())
    		return;
        PlayerEntity playerEntity = harvestCheck.getPlayer();
        DragonStateProvider.getCap(playerEntity).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon()) {
                ItemStack stack = playerEntity.getMainHandItem();
                Item item = stack.getItem();
                BlockState blockState = harvestCheck.getTargetBlock();
                if (!(item instanceof ToolItem || item instanceof SwordItem || item instanceof ShearsItem) && !harvestCheck.canHarvest()) {
                	harvestCheck.setCanHarvest(dragonStateHandler.canHarvestWithPaw(playerEntity, blockState));
            	}
            }
        });
    }

    @SubscribeEvent
    public void disableMounts(EntityMountEvent mountEvent) { //TODO: Add config here
        Entity mounting = mountEvent.getEntityMounting();
        DragonStateProvider.getCap(mounting).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon()) {
                if (mountEvent.getEntityBeingMounted() instanceof AbstractHorseEntity || mountEvent.getEntityBeingMounted() instanceof PigEntity || mountEvent.getEntityBeingMounted() instanceof StriderEntity)
                    mountEvent.setCanceled(true);
            }
        });
    }
    
    @SubscribeEvent
    public void onItemDestroyed(LivingEntityUseItemEvent.Finish destroyItemEvent) {
    	if (!ConfigHandler.SERVER.penalties.get() || ConfigHandler.SERVER.seaTicksWithoutWater.get() == 0)
    		return;
        ItemStack itemStack = destroyItemEvent.getItem();
        DragonStateProvider.getCap(destroyItemEvent.getEntityLiving()).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon()) {
                PlayerEntity playerEntity = (PlayerEntity)destroyItemEvent.getEntityLiving();
                if (ConfigHandler.SERVER.seaAllowWaterBottles.get() && itemStack.getItem() instanceof PotionItem) {
					if (PotionUtils.getPotion(itemStack) == Potions.WATER && dragonStateHandler.getType() == DragonType.SEA && !playerEntity.level.isClientSide) {
						dragonStateHandler.getDebuffData().timeWithoutWater = Math.max(dragonStateHandler.getDebuffData().timeWithoutWater - ConfigHandler.SERVER.seaTicksWithoutWaterRestored.get(), 0);
						NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> playerEntity), new SyncCapabilityDebuff(playerEntity.getId(), dragonStateHandler.getDebuffData().timeWithoutWater, dragonStateHandler.getDebuffData().timeInDarkness, dragonStateHandler.getDebuffData().timeInRain));
					}
                }
                if (SEA_DRAGON_HYDRATION_USE_ALTERNATIVES.contains(itemStack.getItem()) && !playerEntity.level.isClientSide) {
                	dragonStateHandler.getDebuffData().timeWithoutWater = Math.max(dragonStateHandler.getDebuffData().timeWithoutWater - ConfigHandler.SERVER.seaTicksWithoutWaterRestored.get(), 0);
                	NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> playerEntity), new SyncCapabilityDebuff(playerEntity.getId(), dragonStateHandler.getDebuffData().timeWithoutWater, dragonStateHandler.getDebuffData().timeInDarkness, dragonStateHandler.getDebuffData().timeInRain));
                }
            }
        });
    }
	
	@SubscribeEvent
	public void consumeItem(LivingEntityUseItemEvent.Finish destroyItemEvent) {
		if (!ConfigHandler.SERVER.penalties.get())
			return;
		
		if(!(destroyItemEvent.getEntityLiving() instanceof PlayerEntity)) return;
		
		PlayerEntity playerEntity = (PlayerEntity)destroyItemEvent.getEntityLiving();
		ItemStack itemStack = destroyItemEvent.getItem();
		
		DragonStateProvider.getCap(playerEntity).ifPresent(dragonStateHandler -> {
			if (dragonStateHandler.isDragon()) {
				List<String> hurtfulItems = new ArrayList<>(
						dragonStateHandler.getType() == DragonType.FOREST ? ConfigHandler.SERVER.forestDragonHurtfulItems.get() :
						dragonStateHandler.getType() == DragonType.CAVE ? ConfigHandler.SERVER.caveDragonHurtfulItems.get() :
						dragonStateHandler.getType() == DragonType.SEA ? ConfigHandler.SERVER.seaDragonHurtfulItems.get() : new ArrayList<>());

				if(hurtfulItems.size() > 0){
					ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
					
					if(itemId != null){
						for(String item : hurtfulItems){
							boolean match = item.startsWith("item:" + itemId + ":");
							
							if(!match){
								for(ResourceLocation tag : itemStack.getItem().getTags()){
									if(item.startsWith("tag:" + tag + ":")){
										match = true;
										break;
									}
								}
							}
							
							if(match){
								String damage = item.substring(item.lastIndexOf(":") + 1);
								playerEntity.hurt(DamageSource.GENERIC, Float.parseFloat(damage));
								break;
							}
						}
					}
				}
			}
		});
	}
	
	@SubscribeEvent
	public void hitByPotion(ProjectileImpactEvent.Throwable potionEvent) {
		if (!ConfigHandler.SERVER.penalties.get() || ConfigHandler.SERVER.caveSplashDamage.get() == 0.0)
			return;

		if(potionEvent.getThrowable() instanceof PotionEntity){
			PotionEntity potionEntity = (PotionEntity)potionEvent.getThrowable();
			if(potionEntity.getItem().getItem() != Items.SPLASH_POTION) return;
			if(PotionUtils.getPotion(potionEntity.getItem()).getEffects().size() > 0) return; //Remove this line if you want potions with effects to also damage rather then just water ones.
			
			Vector3d pos = potionEvent.getRayTraceResult().getLocation();
			List<PlayerEntity> entities = potionEntity.level.getEntities(EntityType.PLAYER, new AxisAlignedBB(pos.x - 5, pos.y - 1, pos.z - 5, pos.x + 5, pos.y + 1, pos.z + 5), (entity) -> entity.position().distanceTo(pos) <= 4);
			
			for(PlayerEntity player : entities){
				if(player.hasEffect(DragonEffects.FIRE)) continue;
				
				DragonStateProvider.getCap(player).ifPresent(dragonStateHandler -> {
					if (dragonStateHandler.isDragon()) {
						if(dragonStateHandler.getType() != DragonType.CAVE) return;
						player.hurt(DamageSources.WATER_BURN, ConfigHandler.SERVER.caveSplashDamage.get().floatValue());
					}
				});
			}
		}
	}

    @SubscribeEvent
    public void onJump(LivingEvent.LivingJumpEvent jumpEvent) {
        final LivingEntity livingEntity = jumpEvent.getEntityLiving();
        DragonStateProvider.getCap(livingEntity).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon()) {
                switch (dragonStateHandler.getLevel()) {
                    case BABY:
                        livingEntity.push(0, ConfigHandler.SERVER.newbornJump.get(), 0); //1+ block
                        break;
                    case YOUNG:
                        livingEntity.push(0, ConfigHandler.SERVER.youngJump.get(), 0); //1.5+ block
                        break;
                    case ADULT:
                        livingEntity.push(0, ConfigHandler.SERVER.adultJump.get(), 0); //2+ blocks
                        break;
                }
                if (livingEntity instanceof ServerPlayerEntity) {
                    if (livingEntity.getServer().isSingleplayer())
                        NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new StartJump(livingEntity.getId(), 20)); // 42
                    else
                        NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new StartJump(livingEntity.getId(), 10)); // 21
                }
            }
        });
    }
    
    @SubscribeEvent
    public void reduceFallDistance(LivingFallEvent livingFallEvent) {
        LivingEntity livingEntity = livingFallEvent.getEntityLiving();
        DragonStateProvider.getCap(livingEntity).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon()) {
                if (dragonStateHandler.getType() == DragonType.FOREST) {
					float distance = livingFallEvent.getDistance();
					
					if(ConfigHandler.SERVER.bonuses.get()){
						distance -= ConfigHandler.SERVER.forestFallReduction.get().floatValue();
					}
	
	                DragonAbility ability = dragonStateHandler.getAbility(DragonAbilities.CLIFFHANGER);

					if(ability != null){
						distance -= ((CliffhangerAbility)ability).getHeight();
					}
	
	                livingFallEvent.setDistance(distance);
                }
            }
        });
    }
}
