package by.dragonsurvivalteam.dragonsurvival.common.handlers;


import by.dragonsurvivalteam.dragonsurvival.DragonSurvivalMod;
import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.common.dragon_types.AbstractDragonType;
import by.dragonsurvivalteam.dragonsurvival.common.dragon_types.DragonTypes;
import by.dragonsurvivalteam.dragonsurvival.config.obj.ConfigOption;
import by.dragonsurvivalteam.dragonsurvival.config.obj.ConfigSide;
import by.dragonsurvivalteam.dragonsurvival.config.obj.ConfigType;
import by.dragonsurvivalteam.dragonsurvival.config.obj.IgnoreConfigCheck;
import by.dragonsurvivalteam.dragonsurvival.util.DragonUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.config.ModConfigEvent.Loading;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

@Mod.EventBusSubscriber( modid = DragonSurvivalMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD )
public class DragonFoodHandler{


	private static final ResourceLocation FOOD_ICONS = new ResourceLocation(DragonSurvivalMod.MODID + ":textures/gui/dragon_hud.png");
	private static final Random rand = new Random();
	public static CopyOnWriteArrayList<Item> CAVE_D_FOOD;
	public static CopyOnWriteArrayList<Item> FOREST_D_FOOD;
	public static CopyOnWriteArrayList<Item> SEA_D_FOOD;
	public static int rightHeight = 0;

	private static ConcurrentHashMap<String, Map<Item, FoodProperties>> DRAGON_FOODS;

	// Food general
	@ConfigOption( side = ConfigSide.SERVER, category = "food", key = "dragonFoods", comment = "Force dragons to eat a unique diet for their type." )
	public static Boolean customDragonFoods = true;

	// Dragon Food List
	@IgnoreConfigCheck
	@ConfigType( Item.class )
	@ConfigOption( side = ConfigSide.SERVER, category = "food", key = "caveDragon", comment = {"Dragon food formatting: id:food:saturation", "Food/saturation values are optional as the human values will be used if missing."} )
	public static List<String> caveDragonFoods = Arrays.asList("minecraft:coals:1:1", "minecraft:charcoal:1:2", "dragonsurvival:charged_coal:6:1", "dragonsurvival:charred_meat:8:10", "dragonsurvival:cave_dragon_treat:4:8", "dragonsurvival:charred_seafood:7:11", "dragonsurvival:charred_vegetable:8:9", "dragonsurvival:charred_mushroom:9:9", "dragonsurvival:charged_soup:15:15", "dragonsurvival:hot_dragon_rod:4:15", "dragonsurvival:explosive_copper:6:4", "dragonsurvival:double_quartz:8:6", "dragonsurvival:quartz_explosive_copper:12:18", "netherdepthsupgrade:blazefish:6:7", "netherdepthsupgrade:cooked_magmacubefish_slice:2:2", "netherdepthsupgrade:blazefish_slice:2:2", "infernalexp:molten_gold_cluster:2:6", "netherdepthsupgrade:magmacubefish:6:7", "desolation:cinder_fruit:6:7", "desolation:powered_cinder_fruit:8:12", "desolation:activatedcharcoal:2:2", "desolation:infused_powder:10:10", "desolation:primed_ash:7:8", "undergarden:ditchbulb:5,6", "xreliquary:molten_core:1:1", "silents_mechanisms:coal_generator_fuels:1:1", "mekanism:dust_charcoal:1:1", "mekanism:dust_coal:1:1", "rats:nether_cheese", "potionsmaster:charcoal_powder:1:1", "potionsmaster:coal_powder:1:1", "potionsmaster:activated_charcoal:2:2", "thermal:coal_coke:1:1", "infernalexp:glowcoal:2:3", "resourcefulbees:coal_honeycomb:5:5", "resourcefulbees:netherite_honeycomb:5:5", "lazierae2:coal_dust:1:1", "silents_mechanisms:coal_dust:1:1", "potionsmaster:calcinatedcoal_powder:1:1", "thermal:basalz_rod:2:4", "thermal:basalz_powder:1:2", "druidcraft:fiery_glass:2:2", "create:blaze_cake:10:10", "create:creative_blaze_cake:50:50", "upgradednetherite:fire_essence:12:4", "netherdepthsupgrade:fortress_grouper:3:1", "nethersdelight:nether_skewer:6:8");

	@IgnoreConfigCheck
	@ConfigType( Item.class )
	@ConfigOption( side = ConfigSide.SERVER, category = "food", key = "forestDragon", comment = {"Dragon food formatting: id:food:saturation", "Food/saturation values are optional as the human values will be used if missing."} )
	public static List<String> forestDragonFoods = Arrays.asList("forge:raw_meats:6:4", "minecraft:glow_berries:2:1", "minecraft:sweet_berries:1:1", "minecraft:rotten_flesh:2:4", "minecraft:spider_eye:6:8", "minecraft:rabbit:7:8", "minecraft:poisonous_potato:7:8", "minecraft:chorus_fruit:9:8", "minecraft:brown_mushroom:2:1", "minecraft:red_mushroom:2:3", "minecraft:honey_bottle:6:1", "minecraft:warped_fungus:3:3", "minecraft:crimson_fungus:3:3", "dragonsurvival:forest_dragon_treat:4:8", "dragonsurvival:meat_chorus_mix:12:8", "nocubes_better_frogs:raw_frog_leg:4:4", "infernalexp:raw_hogchop:6:6", "phantasm:chorus_fruit_salad:10:10", "dragonsurvival:meat_wild_berries:12:10", "dragonsurvival:smelly_meat_porridge:6:10", "dragonsurvival:diamond_chorus:15:12", "dragonsurvival:luminous_ointment:5:3", "dragonsurvival:sweet_sour_rabbit:10:6", "chinchillas:chinchilla_meat:6:8", "aquaculture:turtle_soup:8:8", "netherdepthsupgrade:wither_bonefish:4:6", "netherdepthsupgrade:bonefish:4:6", "infernalexp:raw_hogchop:8:8", "aoa3:fiery_chops:6:7", "aoa3:raw_chimera_chop:6:7", "aoa3:raw_furlion_chop:6:7", "aoa3:raw_halycon_beef:7:8", "aoa3:raw_charger_shank:6:7", "aoa3:trilliad_leaves:8:11", "pamhc2foodextended:rawtofabbititem", "pamhc2foodextended:rawtofickenitem", "quark:golden_frog_leg:12:14", "pamhc2foodextended:rawtofuttonitem", "alexsmobs:kangaroo_meat:5:6", "alexsmobs:moose_ribs:6:8", "simplefarming:raw_horse_meat:5:6", "simplefarming:raw_bacon:3:3", "simplefarming:raw_chicken_wings:2:3", "simplefarming:raw_sausage:3:4", "xenoclustwo:raw_tortice:7:8", "unnamedanimalmod:musk_ox_shank:7:8", "unnamedanimalmod:frog_legs:5:6", "unnamedanimalmod:mangrove_fruit:4:7", "betteranimalsplus:venisonraw:7:6", "betteranimalsplus:pheasantraw:7:5", "betteranimalsplus:turkey_leg_raw:4:5", "infernalexp:raw_hogchop:6:7", "infernalexp:cured_jerky:10:7", "rats:raw_rat:4:5", "aquaculture:frog:4:5", "aquaculture:frog_legs_raw:4:4", "aquaculture:box_turtle:4:5", "aquaculture:arrau_turtle:4:5", "aquaculture:starshell_turtle:4:5", "undergarden:raw_gloomper_leg:4:5", "undergarden:raw_dweller_meat:6:7", "farmersdelight:chicken_cuts:3:3", "farmersdelight:bacon:3:3", "farmersdelight:ham:9:10", "farmersdelight:minced_beef:5:3", "farmersdelight:mutton_chops:5:3", "abnormals_delight:duck_fillet:2:3", "abnormals_delight:venison_shanks:7:3", "autumnity:foul_berries:2:4", "autumnity:turkey:7:8", "autumnity:turkey_piece:2:4", "autumnity:foul_soup:12:8", "endergetic:bolloom_fruit:3:4", "quark:frog_leg:4:5", "nethers_delight:hoglin_loin:8:6", "nethers_delight:raw_stuffed_hoglin:18:10", "xreliquary:zombie_heart:4:7", "xreliquary:bat_wing:2:2", "eidolon:zombie_heart:7:7", "forbidden_arcanus:bat_wing:5:2", "twilightforest:raw_venison:7:7", "twilightforest:raw_meef:9:5", "twilightforest:hydra_chop", "cyclic:chorus_flight", "cyclic:chorus_spectral", "cyclic:toxic_carrot:15:15", "artifacts:everlasting_beef", "resourcefulbees:rainbow_honey_bottle", "resourcefulbees:diamond_honeycomb:5:5", "byg:soul_shroom:9:5", "byg:death_cap:9:8", "minecolonies:chorus_bread", "wyrmroost:raw_lowtier_meat:3:2", "wyrmroost:raw_common_meat:5:3", "wyrmroost:raw_apex_meat:8:6", "wyrmroost:raw_behemoth_meat:11:12", "wyrmroost:desert_wyrm:4:3", "eanimod:rawchicken_darkbig:9:5", "eanimod:rawchicken_dark:5:4", "eanimod:rawchicken_darksmall:3:2", "eanimod:rawchicken_pale:5:3", "eanimod:rawchicken_palesmall:4:3", "eanimod:rawrabbit_small:4:4", "environmental:duck:4:3", "environmental:venison:7:7", "cnb:lizard_item_jungle:4:4", "cnb:lizard_item_mushroom:4:4", "cnb:lizard_item_jungle_2:4:4", "cnb:lizard_item_desert_2:4:4", "cnb:lizard_egg:5:2", "cnb:lizard_item_desert:4:4", "snowpig:frozen_porkchop:7:3", "snowpig:frozen_ham:5:7", "untamedwilds:spawn_snake:4:4", "untamedwilds:snake_green_mamba:4:4", "untamedwilds:snake_rattlesnake:4:4", "untamedwilds:snake_emerald:4:4", "untamedwilds:snake_carpet_python:4:4", "untamedwilds:snake_corn:4:4", "untamedwilds:snake_gray_kingsnake:4:4", "untamedwilds:snake_coral:4:4", "untamedwilds:snake_ball_python:4:4", "untamedwilds:snake_black_mamba:4:4", "untamedwilds:snake_western_rattlesnake:4:4", "untamedwilds:snake_taipan:4:4", "untamedwilds:snake_adder:4:4", "untamedwilds:snake_rice_paddy:4:4", "untamedwilds:snake_coral_blue:4:4", "untamedwilds:snake_cave_racer:4:4", "untamedwilds:snake_swamp_moccasin:4:4", "untamedwilds:softshell_turtle_pig_nose:4:4", "untamedwilds:softshell_turtle_flapshell:4:4", "untamedwilds:softshell_turtle_chinese:4:4", "untamedwilds:tortoise_asian_box:4:4", "untamedwilds:tortoise_gopher:4:4", "untamedwilds:tortoise_leopard:4:4", "untamedwilds:spawn_softshell_turtle:4:4", "untamedwilds:softshell_turtle_nile:4:4", "untamedwilds:softshell_turtle_spiny:4:4", "untamedwilds:tortoise_sulcata:4:4", "untamedwilds:tortoise_star:4:4", "untamedwilds:spawn_tortoise:4:4", "naturalist:venison:7:6", "leescreatures:raw_boarlin:6:6", "mysticalworld:venison:5:5", "toadterror:toad_chops:8:7", "prehistoricfauna:raw_large_thyreophoran_meat:7:6", "prehistoricfauna:raw_large_marginocephalian_meat:8:6", "prehistoricfauna:raw_small_ornithischian_meat:4:3", "prehistoricfauna:raw_large_sauropod_meat:11:9", "prehistoricfauna:raw_small_sauropod_meat:4:4", "prehistoricfauna:raw_large_theropod_meat:7:7", "prehistoricfauna:raw_small_theropod_meat:4:4", "prehistoricfauna:raw_small_archosauromorph_meat:3:3", "prehistoricfauna:raw_large_archosauromorph_meat:6:5", "prehistoricfauna:raw_small_reptile_meat:4:3", "prehistoricfauna:raw_large_synapsid_meat:5:6", "ends_delight:dragon_leg:15:15", "ends_delight:raw_dragon_meat:10:10", "ends_delight:raw_dragon_meat_cuts:5:2", "ends_delight:dragon_breath_and_chorus_soup:15:15", "ends_delight:ender_sauce:8:15", "ends_delight:raw_ender_mite_meat:1:1", "ends_delight:non_hatchable_dragon_egg:8:5", "ends_delight:shulker_meat:7:4", "unusualend:chorus_juice:2:2", "ends_delight:liquid_dragon_egg:3:3", "ends_delight:shulker_meat_slice:4:1", "unusualend:ender_firefly_egg:2:3", "unusualend:chorus_petal:1:1", "unusualend:chorus_pie:3:4", "unusualend:ender_stew:6:1", "unusualprehistory:kentro_eggs:3:3", "unusualprehistory:hwacha_eggs:3:3", "unusualprehistory:ulugh_eggs:3:3", "unusualprehistory:antarcto_eggs:3:3", "unusualprehistory:austro_eggs:3:3", "unusualprehistory:pachy_eggs:3:3", "unusualprehistory:raptor_eggs:3:3", "unusualprehistory:trike_eggs:4:4", "unusualprehistory:rex_eggs:6:3", "unusualprehistory:coty_eggs:3:3", "unusualprehistory:meaty_buffet:12:15", "unusualprehistory:majunga_eggs:3:3", "unusualprehistory:anuro_eggs:3:3", "unusualprehistory:raw_austro:6:3", "unusualprehistory:raw_coty:5:3", "nourished_nether:raw_hoglin:8:8", "gothic:meat:7:8", "gothic:bug_meat:2:2", "gothic:scavenger_egg:3:3", "gothic:snapperweed:1:1", "gothic:blue_elder:1:1", "unusualend:wandering_stew:1:1", "nethersdelight:raw_stuffed_hoglin:15:15", "nethersdelight:hoglin_ear:1:1", "nethersdelight:ground_strider:6:2", "nethersdelight:strider_slice:8:8", "nethersdelight:hoglin_ear:1:1", "nethersdelight:hoglin_loin:6:6", "nethersdelight:propelpearl:1:1", "orcz:squig_eye:6:6", "orcz:orceye:4:4", "goated:chevon:6:6", "rottencreatures:frozen_rotten_flesh:4:4", "regions_unexplored:tall_green_bioshroom:3:3", "regions_unexplored:green_bioshroom:3:3", "regions_unexplored:blue_bioshroom:3:3", "regions_unexplored:tall_blue_bioshroom:3:3", "regions_unexplored:tall_pink_bioshroom:3:3", "regions_unexplored:pink_bioshroom:3:3", "regions_unexplored:yellow_bioshroom:3:3", "regions_unexplored:tall_yellow_bioshroom:3:3", "quark:glow_shroom:3:3", "gothic:cave_mushrooms:3:3", "gothic:tall_mushrooms:3:3", "gothic:miners_food_wild:3:3", "gothic:black_mushroom:3:3", "phantasm:putac_shroom:3:3", "farmersdelight:red_mushroom_colony:10:3", "farmersdelight:brown_mushroom_colony:10:3", "gothic:miners_food:3:3", "gothic:black_mushroom_item:3:3", "gothic:blue_elder:3:3", "frozen_delight:truffle_slice:4:8", "frozenup:truffle:8:4", "frozen_delight:mushroom_mix:16:3", "nourished_nether:nether_fungus_stew:7:3", "nethersdelight:warped_fungus_colony:10:3", "nethersdelight:crimson_fungus_colony:10:3", "unusualend:chorus_fungus:3:3", "orcz:orcshroom:3:3", "orcz:blue_orcshroom:3:3", "waterstrainer:worm:1:1", "aquaculture:worm:1:1");

	@IgnoreConfigCheck
	@ConfigType( Item.class )
	@ConfigOption( side = ConfigSide.SERVER, category = "food", key = "seaDragon", comment = {"Dragon food formatting: id:food:saturation", "Food/saturation values are optional as the human values will be used if missing."} )
	public static List<String> seaDragonFoods = Arrays.asList("forge:raw_fishes:6:4", "minecraft:kelp:1:1", "minecraft:pufferfish:8:8", "dragonsurvival:sea_dragon_treat:4:8", "dragonsurvival:seasoned_fish:12:10", "dragonsurvival:golden_coral_pufferfish:12:14", "dragonsurvival:frozen_raw_fish:2:1", "dragonsurvival:golden_turtle_egg:15:12", "aoa3:raw_candlefish:9:9", "aoa3:raw_crimson_skipper:8:8", "aoa3:raw_fingerfish:4:4", "aoa3:raw_pearl_stripefish:5:4", "aoa3:raw_limefish:5:5", "aoa3:raw_sailback:6:5", "netherdepthsupgrade:soulsucker:6:7", "netherdepthsupgrade:obsidianfish:6:7", "netherdepthsupgrade:lava_pufferfish:8:7", "netherdepthsupgrade:searing_cod:6:7", "netherdepthsupgrade:glowdine:6:7", "netherdepthsupgrade:warped_kelp:2:2", "netherdepthsupgrade:lava_pufferfish_slice:2:2", "netherdepthsupgrade:glowdine_slice:2:2", "netherdepthsupgrade:soulsucker_slice:2:2", "netherdepthsupgrade:obsidianfish_slice:2:2", "netherdepthsupgrade:searing_cod_slice:2:2", "crittersandcompanions:clam:10:3", "aoa3:raw_golden_gullfish:10:2", "aoa3:raw_turquoise_stripefish:7:6", "aoa3:raw_violet_skipper:7:7", "aoa3:raw_rocketfish:4:10", "aoa3:raw_crimson_stripefish:8:7", "aoa3:raw_sapphire_strider:9:8", "aoa3:raw_dark_hatchetfish:9:9", "aoa3:raw_ironback:10:9", "aoa3:raw_rainbowfish:11:11", "aoa3:raw_razorfish:12:14", "alexsmobs:lobster_tail:4:5", "alexsmobs:blobfish:8:9", "oddwatermobs:raw_ghost_shark:8:8", "oddwatermobs:raw_isopod:4:2", "oddwatermobs:raw_mudskipper:6:7", "oddwatermobs:raw_coelacanth:9:10", "oddwatermobs:raw_anglerfish:6:6", "oddwatermobs:deep_sea_fish:4:2", "oddwatermobs:crab_leg:5:6", "simplefarming:raw_calamari:5:6", "unnamedanimalmod:elephantnose_fish:5:6", "unnamedanimalmod:flashlight_fish:5:6", "unnamedanimalmod:rocket_killifish:5:6", "unnamedanimalmod:leafy_seadragon:5:6", "unnamedanimalmod:elephantnose_fish:5:6", "betteranimalsplus:eel_meat_raw:5:6", "betteranimalsplus:calamari_raw:4:5", "betteranimalsplus:crab_meat_raw:4:4", "aquaculture:fish_fillet_raw:2:2", "aquaculture:goldfish:8:4", "aquaculture:algae:3:2", "betterendforge:end_fish_raw:6:7", "betterendforge:hydralux_petal:3:3", "betterendforge:charnia_green:2:2", "shroomed:raw_shroomfin:5:6", "undergarden:raw_gwibling:5:6", "bettas:betta_fish:4:5", "quark:crab_leg:4:4", "pamhc2foodextended:rawtofishitem", "fins:banded_redback_shrimp:6:1", "fins:night_light_squid:6:2", "fins:night_light_squid_tentacle:6:2", "fins:emerald_spindly_gem_crab:7:2", "fins:amber_spindly_gem_crab:7:2", "fins:rubby_spindly_gem_crab:7:2", "fins:sapphire_spindly_gem_crab:7:2", "fins:pearl_spindly_gem_crab:7:2", "fins:papa_wee:6:2", "fins:bugmeat:4:2", "fins:raw_golden_river_ray_wing:6:2", "fins:red_bull_crab_claw:4:4", "fins:white_bull_crab_claw:4:4", "fins:wherble_fin:1:1", "forbidden_arcanus:tentacle:5:2", "pneumaticcraft:raw_salmon_tempura:6:10", "rats:ratfish:4:2", "upgrade_aquatic:purple_pickerelweed:2:2", "upgrade_aquatic:blue_pickerelweed:2:2", "upgrade_aquatic:polar_kelp:2:2", "upgrade_aquatic:tongue_kelp:2:2", "upgrade_aquatic:thorny_kelp:2:2", "upgrade_aquatic:ochre_kelp:2:2", "upgrade_aquatic:lionfish:8:9", "aquaculture:sushi:6:5", "freshwarriors:fresh_soup:15:10", "freshwarriors:beluga_caviar:10:3", "freshwarriors:piranha:4:1", "freshwarriors:tilapia:4:1", "freshwarriors:stuffed_piranha:4:1", "freshwarriors:tigerfish:5:5", "freshwarriors:toe_biter_leg:3:3", "untamedwilds:egg_arowana:4:4", "untamedwilds:egg_trevally_jack:4:4", "untamedwilds:egg_trevally:4:4", "untamedwilds:egg_giant_salamander:6:4", "untamedwilds:egg_giant_salamander_hellbender:6:4", "untamedwilds:egg_giant_salamander_japanese:6:4", "untamedwilds:giant_clam:4:4", "untamedwilds:giant_clam_derasa:4:4", "untamedwilds:giant_clam_maxima:4:4", "untamedwilds:giant_clam_squamosa:4:4", "untamedwilds:egg_trevally_giant:6:4", "untamedwilds:egg_trevally:6:4", "untamedwilds:egg_trevally_bigeye:6:4", "untamedwilds:egg_sunfish:6:4", "untamedwilds:egg_sunfish_sunfish:6:4", "untamedwilds:egg_giant_clam_squamosa:6:4", "untamedwilds:egg_giant_clam_gigas:6:4", "untamedwilds:egg_giant_clam_derasa:6:4", "untamedwilds:egg_giant_clam:6:4", "untamedwilds:egg_football_fish:6:4", "untamedwilds:egg_arowana:6:4", "untamedwilds:egg_arowana_jardini:6:4", "untamedwilds:egg_arowana_green:6:4", "mysticalworld:raw_squid:6:5", "aquafina:fresh_soup:10:10", "aquafina:beluga_caviar:10:3", "aquafina:raw_piranha:4:1", "aquafina:raw_tilapia:4:1", "aquafina:stuffed_piranha:4:1", "aquafina:tigerfish:5:5", "aquafina:toe_biter_leg:3:3", "aquafina:raw_angelfish:4:1", "aquafina:raw_football_fish:4:1", "aquafina:raw_foxface_fish:4:1", "aquafina:raw_royal_gramma:4:1", "aquafina:raw_starfish:4:1", "aquafina:spider_crab_leg:4:1", "aquafina:raw_stingray_slice:4:1", "prehistoricfauna:raw_ceratodus:5:5", "prehistoricfauna:raw_cyclurus:4:4", "prehistoricfauna:raw_potamoceratodus:5:5", "prehistoricfauna:raw_myledaphus:4:4", "prehistoricfauna:raw_gar:4:4", "prehistoricfauna:raw_oyster:4:3", "prehistoric_delight:prehistoric_fillet:3:3", "seadwellers:rainbow_trout:10:10", "crittersandcompanions:koi_fish:5:5", "aquamirae:elodea:3:3", "croptopia:clam:3:3", "croptopia:calamari:2:3", "croptopia:anchovy:3:2", "croptopia:crab:6:8", "croptopia:glowing_calamari:4:5", "croptopia:oyster:2:4", "croptopia:roe:1:2", "croptopia:shrimp:2:2", "croptopia:tuna:6:4", "aquamirae:spinefish:4:4", "alexsmobs:flying_fish:6:4", "untamedwilds:egg_triggerfish:2:4", "untamedwilds:egg_catfish:2:4", "netherdepthsupgrade:eyeball:3:3", "netherdepthsupgrade:eyeball_fish:3:3", "oceansdelight:guardian:4:3", "oceansdelight:guardian_tail:1:3", "oceansdelight:cut_tentacles:3:1", "oceansdelight:tentacles:3:4", "oceansdelight:tentacle_on_a_stick:3:4", "oceansdelight:fugu_slice:5:4", "oceansdelight:elder_guardian_slice:8:6", "oceansdelight:elder_guardian_slab:15:15", "upgrade_aquatic:elder_eye:15:15", "unusualprehistory:golden_scau:15:15", "unusualprehistory:raw_scau:4:3", "unusualprehistory:raw_stetha:4:3", "unusualprehistory:stetha_eggs:4:3", "unusualprehistory:beelze_eggs:4:3", "unusualprehistory:scau_eggs:4:3", "unusualprehistory:ammon_eggs:4:3", "unusualprehistory:dunk_eggs:4:3");

	@ConfigOption( side = ConfigSide.SERVER, key = "foodHungerEffect", category = "food", comment = "Should eating wrong food items give hunger effect?" )
	public static boolean foodHungerEffect = true;

	@SubscribeEvent
	public static void onConfigLoad(Loading event){
		if(event.getConfig().getType() == Type.SERVER){
			rebuildFoodMap();
		}
	}


	private static void rebuildFoodMap(){
		ConcurrentHashMap<String, ConcurrentHashMap<Item, FoodProperties>> dragonMap = new ConcurrentHashMap<String, ConcurrentHashMap<Item, FoodProperties>>();
		dragonMap.put(DragonTypes.CAVE.getTypeName(), buildDragonFoodMap(DragonTypes.CAVE));
		dragonMap.put(DragonTypes.FOREST.getTypeName(), buildDragonFoodMap(DragonTypes.FOREST));
		dragonMap.put(DragonTypes.SEA.getTypeName(), buildDragonFoodMap(DragonTypes.SEA));
		DRAGON_FOODS = new ConcurrentHashMap<>(dragonMap);
	}


	private static ConcurrentHashMap<Item, FoodProperties> buildDragonFoodMap(AbstractDragonType type){
		ConcurrentHashMap<Item, FoodProperties> foodMap = new ConcurrentHashMap<Item, FoodProperties>();

		if(!customDragonFoods){
			return foodMap;
		}
		String[] configFood = new String[0];
		
		if(Objects.equals(type, DragonTypes.CAVE)){
			configFood = caveDragonFoods.toArray(new String[0]);
		}

		if(Objects.equals(type, DragonTypes.FOREST)){
			configFood = forestDragonFoods.toArray(new String[0]);
		}
		if(Objects.equals(type, DragonTypes.SEA)){
			configFood = seaDragonFoods.toArray(new String[0]);
		}
		

		configFood = Stream.of(configFood).sorted(Comparator.reverseOrder()).toArray(String[]::new);
		for(String entry : configFood){
			if(entry.startsWith("item:")){
				entry = entry.substring("item:".length());
			}

			if(entry.startsWith("tag:")){
				entry = entry.substring("tag:".length());
			}

			String[] sEntry = entry.split(":");
			ResourceLocation rlEntry = new ResourceLocation(sEntry[0], sEntry[1]);

			TagKey<Item> tagKey = TagKey.create(Registry.ITEM_REGISTRY, rlEntry);
			if(ForgeRegistries.ITEMS.tags().isKnownTagName(tagKey)){
				ForgeRegistries.ITEMS.tags().getTag(tagKey).forEach(item -> {
					FoodProperties FoodProperties = calculateDragonFoodProperties(item, type, sEntry.length == 4 ? Integer.parseInt(sEntry[2]) : item.getFoodProperties() != null ? item.getFoodProperties().getNutrition() : 1, sEntry.length == 4 ? Integer.parseInt(sEntry[3]) : item.getFoodProperties() != null ? (int)(item.getFoodProperties().getNutrition() * (item.getFoodProperties().getSaturationModifier() * 2.0F)) : 0, true);
					if(FoodProperties != null){
						foodMap.put(item, FoodProperties);
					}
				});
			}

			if(ForgeRegistries.ITEMS.containsKey(rlEntry)){
				Item item = ForgeRegistries.ITEMS.getValue(rlEntry);

				if(item != null && item != Items.AIR){
					FoodProperties FoodProperties = calculateDragonFoodProperties(item, type, sEntry.length == 4 ? Integer.parseInt(sEntry[2]) : item.getFoodProperties() != null ? item.getFoodProperties().getNutrition() : 1, sEntry.length == 4 ? Integer.parseInt(sEntry[3]) : item.getFoodProperties() != null ? (int)(item.getFoodProperties().getNutrition() * (item.getFoodProperties().getSaturationModifier() * 2.0F)) : 0, true);

					if(FoodProperties != null){
						foodMap.put(item, FoodProperties);
					}
				}else{
					//	DragonSurvivalMod.LOGGER.warn("Unknown item '{}:{}' in {} dragon FoodProperties config.", sEntry[1], sEntry[2], type.toString().toLowerCase());
				}
			}
		}

		for(Item item : ForgeRegistries.ITEMS.getValues()){
			if(!foodMap.containsKey(item) && item.isEdible()){
				FoodProperties FoodProperties = calculateDragonFoodProperties(item, type, 0, 0, false);

				if(FoodProperties != null){

					foodMap.put(item, FoodProperties);
				}
			}
		}
		return new ConcurrentHashMap<>(foodMap);
	}

	@Nullable
	private static FoodProperties calculateDragonFoodProperties(Item item, AbstractDragonType type, int nutrition, int saturation, boolean dragonFood){
		if(item == null){
			return new FoodProperties.Builder().nutrition(nutrition).saturationMod((float)saturation / (float)nutrition / 2.0F).build();
		}

		if(!customDragonFoods || type == null){
			return item.getFoodProperties();
		}

		FoodProperties.Builder builder = new FoodProperties.Builder();
		FoodProperties humanFood = item.getFoodProperties();

		if(humanFood != null){
			if(humanFood.isMeat()){
				builder.meat();
			}
			if(humanFood.canAlwaysEat()){
				builder.alwaysEat();
			}
			if(humanFood.isFastFood()){
				builder.fast();
			}

			for(Pair<MobEffectInstance, Float> effect : humanFood.getEffects()){
				if(effect == null || effect.getFirst() == null){
					continue;
				}
				if(effect.getFirst().getEffect() != MobEffects.HUNGER && effect.getFirst().getEffect() != MobEffects.POISON && dragonFood){
					builder.effect(effect::getFirst, effect.getSecond());
				}
			}
		}

		if(!dragonFood && foodHungerEffect){
			builder.effect(() -> new MobEffectInstance(MobEffects.HUNGER, 20 * 60, 0), 1.0F);
		}

		if (saturation == 0 || nutrition == 0)
			builder.nutrition(nutrition).saturationMod(0.0F);
		else
			builder.nutrition(nutrition).saturationMod((float)saturation / (float)nutrition / 2.0F);

		return builder.build();
	}

	public static CopyOnWriteArrayList<Item> getSafeEdibleFoods(AbstractDragonType dragonType){
		if(dragonType != null){
			if(Objects.equals(dragonType, DragonTypes.FOREST) && FOREST_D_FOOD != null){
				return FOREST_D_FOOD;
			}else if(Objects.equals(dragonType, DragonTypes.SEA) && SEA_D_FOOD != null){
				return SEA_D_FOOD;
			}else if(Objects.equals(dragonType, DragonTypes.CAVE) && CAVE_D_FOOD != null){
				return CAVE_D_FOOD;
			}
		}

		if(DRAGON_FOODS == null){
			rebuildFoodMap();
		}

		CopyOnWriteArrayList<Item> foods = new CopyOnWriteArrayList<>();
		for(Item item : DRAGON_FOODS.get(dragonType.getTypeName()).keySet()){
			boolean safe = true;
			final FoodProperties FoodProperties = DRAGON_FOODS.get(dragonType.getTypeName()).get(item);
			if(FoodProperties != null){
				for(Pair<MobEffectInstance, Float> effect : FoodProperties.getEffects()){
					if(effect == null || effect.getFirst() == null){
						continue;
					}
					MobEffect e = effect.getFirst().getEffect();
					if(!e.isBeneficial() && e != MobEffects.CONFUSION){ // Because we decided to leave confusion on pufferfish
						safe = false;
						break;
					}
				}
				if(safe){
					foods.add(item);
				}
			}
		}
		if(Objects.equals(dragonType, DragonTypes.FOREST) && FOREST_D_FOOD == null){
			FOREST_D_FOOD = foods;
		}else if(Objects.equals(dragonType, DragonTypes.CAVE) && CAVE_D_FOOD == null){
			CAVE_D_FOOD = foods;
		}else if(Objects.equals(dragonType, DragonTypes.SEA) && SEA_D_FOOD == null){
			SEA_D_FOOD = foods;
		}
		return foods;
	}

	public static void dragonEat(FoodData foodStats, Item item, ItemStack itemStack, AbstractDragonType type){
		if(isDragonEdible(item, type)){
			FoodProperties FoodProperties = getDragonFoodProperties(item, type);
			foodStats.eat(FoodProperties.getNutrition(), FoodProperties.getSaturationModifier());
		}
	}

	@Nullable
	public static FoodProperties getDragonFoodProperties(Item item, AbstractDragonType type){
		if(DRAGON_FOODS == null || !customDragonFoods || type == null){

			return item.getFoodProperties();
		}
		if(DRAGON_FOODS.get(type.getTypeName()).containsKey(item)){
			return DRAGON_FOODS.get(type.getTypeName()).get(item);
		}
		return null;
	}

	public static boolean isDragonEdible(Item item, AbstractDragonType type){
		if(customDragonFoods && type != null){
			return DRAGON_FOODS != null && DRAGON_FOODS.containsKey(type.getTypeName()) && item != null && DRAGON_FOODS.get(type.getTypeName()).containsKey(item);
		}
		return item.getFoodProperties() != null;
	}

	@OnlyIn( Dist.CLIENT )
	public static void onRenderFoodBar(ForgeGui gui, PoseStack mStack, float partialTicks, int width, int height){
		LocalPlayer player = Minecraft.getInstance().player;

		if(Minecraft.getInstance().options.hideGui || !gui.shouldDrawSurvivalElements()){
			return;
		}
		if(!customDragonFoods || !DragonUtils.isDragon(player)){
			VanillaGuiOverlay.FOOD_LEVEL.type().overlay().render(gui, mStack, partialTicks, width, height);
			return;
		}

		DragonStateProvider.getCap(player).ifPresent(dragonStateHandler -> {
			if(dragonStateHandler.isDragon()){

				rand.setSeed(player.tickCount * 312871L);

				RenderSystem.enableBlend();
				RenderSystem.setShaderTexture(0, FOOD_ICONS);

				if(Minecraft.getInstance().gui instanceof ForgeGui){
					rightHeight = ((ForgeGui)Minecraft.getInstance().gui).rightHeight;
					((ForgeGui)Minecraft.getInstance().gui).rightHeight += 10;
				}

				final int left = width / 2 + 91;
				final int top = height - rightHeight;
				rightHeight += 10;
				final FoodData food = player.getFoodData();

				final int type = Objects.equals(dragonStateHandler.getType(), DragonTypes.FOREST) ? 0 : Objects.equals(dragonStateHandler.getType(), DragonTypes.CAVE) ? 9 : 18;

				final boolean hunger = player.hasEffect(MobEffects.HUNGER);

				for(int i = 0; i < 10; ++i){
					int idx = i * 2 + 1;
					int y = top;

					if(food.getSaturationLevel() <= 0.0F && player.tickCount % (food.getFoodLevel() * 3 + 1) == 0){
						y = top + rand.nextInt(3) - 1;
					}

					gui.blit(mStack, left - i * 8 - 9, y, hunger ? 117 : 0, type, 9, 9);

					if(idx < food.getFoodLevel()){
						gui.blit(mStack, left - i * 8 - 9, y, hunger ? 72 : 36, type, 9, 9);
					}else if(idx == food.getFoodLevel()){
						gui.blit(mStack, left - i * 8 - 9, y, hunger ? 81 : 45, type, 9, 9);
					}
				}

				RenderSystem.setShaderTexture(0, Gui.GUI_ICONS_LOCATION);
				RenderSystem.disableBlend();
			}else{
			}
		});
	}

	@SubscribeEvent
	public void onItemUseStart(LivingEntityUseItemEvent.Start event){
		DragonStateProvider.getCap(event.getEntity()).ifPresent(dragonStateHandler -> {
			if(dragonStateHandler.isDragon()){
				event.setDuration(getUseDuration(event.getItem(), dragonStateHandler.getType()));
			}
		});
	}

	public static int getUseDuration(ItemStack item, AbstractDragonType type){
		if(isDragonEdible(item.getItem(), type)){
			return item.getItem().getFoodProperties() != null && item.getItem().getFoodProperties().isFastFood() ? 16 : 32;
		}else{
			return item.getUseDuration(); // VERIFY THIS
		}
	}

	@SubscribeEvent
	public void onItemRightClick(PlayerInteractEvent.RightClickItem event){
		DragonStateProvider.getCap(event.getEntity()).ifPresent(dragonStateHandler -> {

			if(dragonStateHandler.isDragon()){
				if(!event.getEntity().level.isClientSide){
					ServerPlayer player = (ServerPlayer)event.getEntity();
					ServerLevel level = player.getLevel();
					InteractionHand hand = event.getHand();

					ItemStack stack = player.getItemInHand(event.getHand());
					if(isDragonEdible(stack.getItem(), dragonStateHandler.getType())){
						int i = stack.getCount();
						int j = stack.getDamageValue();
						InteractionResultHolder<ItemStack> actionresult = stack.use(level, player, hand);
						ItemStack itemstack = actionresult.getObject();
						if(itemstack == stack && itemstack.getCount() == i && getUseDuration(itemstack, dragonStateHandler.getType()) <= 0 && itemstack.getDamageValue() == j){
							{
								event.setCancellationResult(actionresult.getResult());
							}
						}else if(actionresult.getResult() == InteractionResult.FAIL && getUseDuration(itemstack, dragonStateHandler.getType()) > 0 && !player.isUsingItem()){

							{
								event.setCancellationResult(actionresult.getResult());
								event.setCanceled(true);
							}
						}else{
							player.setItemInHand(hand, itemstack);
							if(player.isCreative()){
								itemstack.setCount(i);
								if(itemstack.isDamageableItem() && itemstack.getDamageValue() != j){
									itemstack.setDamageValue(j);
								}
							}

							if(itemstack.isEmpty()){
								player.setItemInHand(hand, ItemStack.EMPTY);
							}


							event.setCancellationResult(actionresult.getResult());
							event.setCanceled(true);
						}
					}
				}
			}
		});
	}
}