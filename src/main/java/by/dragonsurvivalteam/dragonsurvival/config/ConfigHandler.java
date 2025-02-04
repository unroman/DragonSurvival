package by.dragonsurvivalteam.dragonsurvival.config;

import by.dragonsurvivalteam.dragonsurvival.DragonSurvivalMod;
import by.dragonsurvivalteam.dragonsurvival.config.obj.ConfigOption;
import by.dragonsurvivalteam.dragonsurvival.config.obj.ConfigRange;
import by.dragonsurvivalteam.dragonsurvival.config.obj.ConfigSide;
import by.dragonsurvivalteam.dragonsurvival.config.obj.IgnoreConfigCheck;
import com.electronwill.nightconfig.core.EnumGetMethod;
import com.google.common.primitives.Primitives;
import com.google.gson.reflect.TypeToken;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.moddiscovery.ModAnnotation.EnumHolder;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.tags.ITagManager;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Supplier;

@EventBusSubscriber( modid = DragonSurvivalMod.MODID, bus = Bus.MOD )
public class ConfigHandler{

	public static ClientConfig CLIENT;
	public static ForgeConfigSpec clientSpec;
	public static ServerConfig SERVER;
	public static ForgeConfigSpec serverSpec;

	public static HashMap<String, Object> defaultConfigValues = new HashMap<>();
	public static HashMap<String, ConfigOption> configObjects = new HashMap<>();
	public static HashMap<String, Field> configFields = new HashMap<>();
	public static HashMap<ConfigSide, List<String>> configs = new HashMap<>();
	public static HashMap<String, ForgeConfigSpec.ConfigValue> configValues = new HashMap<>();

	private static List<Field> getFields() {
		List<Field> instances = new ArrayList<>();

		Type annotationType = Type.getType(ConfigOption.class);
		ModList.get().getAllScanData().forEach(s -> {
			List<ModFileScanData.AnnotationData> ebsTargets = s.getAnnotations().stream().filter(s1 -> s1.targetType() == ElementType.FIELD).filter(annotationData -> annotationType.equals(annotationData.annotationType())).toList();

			ebsTargets.forEach(ad ->  {
				EnumHolder sidesValue = (EnumHolder)ad.annotationData().get("side");
				Dist side = Objects.equals(sidesValue.getValue(), "CLIENT") ? Dist.CLIENT : Dist.DEDICATED_SERVER;

				if(side == FMLEnvironment.dist || side == Dist.DEDICATED_SERVER){
					try{
						Class<?> c = Class.forName(ad.clazz().getClassName());
						Field fe = c.getDeclaredField(ad.memberName());
						instances.add(fe);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			});
		});
		return instances;
	}

	public static void initConfig(){
		initTypes();
		List<Field> set = getFields();

		set.forEach(s -> {
			if(!Modifier.isStatic(s.getModifiers()))
				return;

			ConfigOption option = s.getAnnotation(ConfigOption.class);

			try{
				defaultConfigValues.put(option.key(), s.get(null));
			}catch(IllegalAccessException e){
				e.printStackTrace();
			}
			configFields.put(option.key(), s);
			configObjects.put(option.key(), option);

			configs.computeIfAbsent(option.side(), c -> new ArrayList<>());
			configs.get(option.side()).add(option.key());
		});

		if(FMLEnvironment.dist.isClient()){
			Pair<ClientConfig, ForgeConfigSpec> client = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
			CLIENT = client.getLeft();
			clientSpec = client.getRight();
			ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientSpec);
		}

		Pair<ServerConfig, ForgeConfigSpec> server = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
		SERVER = server.getLeft();
		serverSpec = server.getRight();

		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, serverSpec);
	}

	public static void addConfigs(ForgeConfigSpec.Builder builder, ConfigSide side){
		for(String key : configs.getOrDefault(side, Collections.emptyList())){
			ConfigOption option = configObjects.get(key);
			Field fe = configFields.get(key);
			Object defaultOb = defaultConfigValues.get(option.key());

			String[] categories = option.category() != null && option.category().length > 0 ? option.category() : new String[]{"general"};
			String[] comment =  option.comment() != null ? option.comment() : new String[0];

			for(String string : categories){
				builder.push(string);
			}

			builder.comment(comment);

			if(!option.localization().isBlank()){
				builder.translation(option.localization());
			}

			try{
				Object tt = Primitives.isWrapperType(defaultOb.getClass()) ? Primitives.wrap(defaultOb.getClass()).cast(defaultOb) : defaultOb;

				ConfigRange range = fe.isAnnotationPresent(ConfigRange.class) ? fe.getAnnotation(ConfigRange.class) : null;
				boolean rang = range != null;

				if(tt instanceof Integer intVal){
					IntValue value = builder.defineInRange(option.key(), intVal, rang ? (int)range.min() : Integer.MIN_VALUE, rang ? (int)range.max() : Integer.MAX_VALUE);
					configValues.put(key, value);
				}else if(tt instanceof Float floatVal){
					DoubleValue value = builder.defineInRange(option.key(), floatVal, rang ? range.min() : Float.MIN_VALUE, rang ? range.max() : Float.MAX_VALUE);
					configValues.put(key, value);
				}else if(tt instanceof Long longVal){
					LongValue value = builder.defineInRange(option.key(), longVal, rang ? (long)range.min() : Long.MIN_VALUE, rang ? (long)range.max() : Long.MAX_VALUE);
					configValues.put(key, value);
				}else if(tt instanceof Double doubleVal){
					DoubleValue value = builder.defineInRange(option.key(), doubleVal, rang ? range.min() : Double.MIN_VALUE, rang ? range.max() : Double.MAX_VALUE);
					configValues.put(key, value);
				}else if(tt instanceof Boolean boolValue){
					BooleanValue value = builder.define(option.key(), (boolean)boolValue);
					configValues.put(key, value);
				}else if(fe.getType().isEnum()){
					EnumValue value = builder.defineEnum(option.key(), (Enum)defaultOb, ((Enum<?>)defaultOb).getClass().getEnumConstants());
					configValues.put(key, value);
				}else if(tt instanceof List<?> ls){
					ConfigValue<List<?>> value = builder.defineList(option.key(), ls, s -> fe.isAnnotationPresent(IgnoreConfigCheck.class) || ResourceLocation.isValidResourceLocation(String.valueOf(s)));
					configValues.put(key, value);
				}else{
					ConfigValue<Object> value = builder.define(option.key(), defaultOb);
					configValues.put(key, value);
					System.out.println("Possible config issue for option: " + option.key());
				}
			}catch(Exception e){
				e.printStackTrace();
			}


			for(int i = 0; i < categories.length; i++){
				builder.pop();
			}
		}
	}

	private static Object convertObject(Object ob){
		if(ob instanceof IForgeRegistry<?> forge){
			return forge.getRegistryName().toString();
		}

		if(ob instanceof Enum<?>){
			return ((Enum<?>)ob).name();
		}

		return ob;
	}

	private static final HashMap<Class<?>, Tuple<Supplier<IForgeRegistry<?>>, Supplier<ResourceKey<? extends Registry<?>>>>> REGISTRY_HASH_MAP = new HashMap<>();
	public static void initTypes(){
		REGISTRY_HASH_MAP.put(Item.class, new Tuple<>(() -> ForgeRegistries.ITEMS, () -> Registry.ITEM_REGISTRY));
		REGISTRY_HASH_MAP.put(Block.class,  new Tuple<>(() -> ForgeRegistries.BLOCKS, () -> Registry.BLOCK_REGISTRY));
		REGISTRY_HASH_MAP.put(EntityType.class,  new Tuple<>(() -> ForgeRegistries.ENTITY_TYPES, () -> Registry.ENTITY_TYPE_REGISTRY));
		REGISTRY_HASH_MAP.put(BlockEntityType.class, new Tuple<>(() -> ForgeRegistries.BLOCK_ENTITY_TYPES, () -> Registry.BLOCK_ENTITY_TYPE_REGISTRY));
		REGISTRY_HASH_MAP.put(Biome.class,  new Tuple<>(() -> ForgeRegistries.BIOMES, () -> Registry.BIOME_REGISTRY));
		REGISTRY_HASH_MAP.put(MobEffect.class,  new Tuple<>(() -> ForgeRegistries.MOB_EFFECTS, () -> Registry.MOB_EFFECT_REGISTRY));
		REGISTRY_HASH_MAP.put(Potion.class,  new Tuple<>(() -> ForgeRegistries.POTIONS, () -> Registry.POTION_REGISTRY));
	}

	public static <T> List<T> parseObject(Class<T> type, ResourceLocation location){
		Tuple<Supplier<IForgeRegistry<?>>, Supplier<ResourceKey<? extends Registry<?>>>> ent = REGISTRY_HASH_MAP.getOrDefault(type, null);
		if(ent != null){
			if(ForgeRegistries.ITEMS.containsKey(location)){
				Optional<? extends Holder<?>> optional = ent.getA().get().getHolder(location);
				if(optional.isPresent() && optional.get().isBound()){
					return List.of((T)optional.get().value());
				}
			}else{
				TagKey<T> tagKey = TagKey.create((ResourceKey<? extends Registry<T>>)ent.getB().get(), location);

				if(tagKey.isFor(ent.getA().get().getRegistryKey())){
					ITagManager<T> manager = (ITagManager<T>)ent.getA().get().tags();
					return manager.getTag(tagKey).stream().toList();
				}
			}
		}

		return Collections.emptyList();
	}

	private static Object loadObject(Field fe, Object ob){
		if(ob instanceof String key){
			if(fe.getType().isEnum()){
				Class<? extends Enum> cs = (Class<? extends Enum>)fe.getType();
				return EnumGetMethod.ORDINAL_OR_NAME.get(ob, cs);
			}

			ResourceLocation location = ResourceLocation.tryParse(key);
			List<?> ls = parseObject((Class<? extends IForgeRegistry>)fe.getType(), location);

			if(ls != null){
				if(fe.getGenericType() instanceof List<?>){
					return ls;
				}else{
					return ls.stream().findFirst().orElseGet(() -> null);
				}
			}
		}

		if(ob instanceof Double db){
			if(fe.getType().equals(int.class) || fe.getType().equals(Integer.class)){
				return db.intValue();
			}

			if(fe.getType().equals(float.class) || fe.getType().equals(Float.class)){
				return db.floatValue();
			}

			if(fe.getType().equals(long.class) || fe.getType().equals(Long.class)){
				return db.longValue();
			}

			return db;
		}

		return ob;
	}

	public static boolean isType(Field fe, Class<?> c){
		TypeToken<?> check = TypeToken.get(c);
		TypeToken<?> token = TypeToken.get(fe.getType());

		if(fe.getGenericType() instanceof ParameterizedType prType){
			TypeToken<?> token1 = TypeToken.get(prType.getActualTypeArguments()[0]);
			if(check.isAssignableFrom(token1)) return true;
		}

		return check.isAssignableFrom(token);
	}

	@SubscribeEvent
	public static void onModConfig(ModConfigEvent.Reloading event){
		onModConfig(event.getConfig().getType());
	}

	@SubscribeEvent
	public static void onModConfig(ModConfigEvent.Loading event){
		onModConfig(event.getConfig().getType());
	}

	public static void onModConfig(ModConfig.Type type){
		ConfigSide side = type == ModConfig.Type.SERVER ? ConfigSide.SERVER : ConfigSide.CLIENT;
		List<String> configList = configs.get(side);
		for(String s : configList){
			try{
				if(configValues.containsKey(s) && configFields.containsKey(s)){
					Field fe = ConfigHandler.configFields.get(s);

					if(fe != null){
						Object obj = convertFromString(fe, configValues.get(s).get());

						if(obj != null){
							fe.set(null, obj);
						}
					}
				}
			}catch(IllegalAccessException e){
				e.printStackTrace();
			}
		}
	}

	public static void updateConfigValue(String conf, Object value){
		if(configValues.containsKey(conf)){
			updateConfigValue(configValues.get(conf), value);
		}
	}

	public static void updateConfigValue(ForgeConfigSpec.ConfigValue conf, Object value){
		Util.ioPool().execute(() -> {
			conf.set(convertToString(value));
			conf.save();
		});

		ConfigHandler.configValues.entrySet().stream().filter(c -> c.getValue() == conf).findFirst().ifPresent(key -> {
			try{
				Field fe = ConfigHandler.configFields.get(key.getKey());

				if(fe != null){
					Object obj = convertFromString(fe, value);

					if(obj != null){
						fe.set(null, obj);
					}
				}
			}catch(IllegalAccessException e){
				e.printStackTrace();
			}
		});
	}

	@Nullable
	private static Object convertToString(Object ob){
		if(ob instanceof Collection<?>){
			Collection<Object> obs = new ArrayList<>();

			for(Object o : (Collection<?>)ob){
				obs.add(convertObject(o));
			}

			ob = obs;
		}else{
			ob = convertObject(ob);
		}
		return ob;
	}

	private static Object convertFromString(Field fe, Object obj) throws IllegalAccessException{
		if(fe.getType().isAssignableFrom(Collection.class)){
			ArrayList<Object> ls = new ArrayList<>();

			for(Object o : (Collection)obj){
				ls.add(loadObject(fe, o));
			}

			obj = ls;
		}

		return loadObject(fe, obj);
	}

	public static <T> List<T> configList(Class<T> type, List<?> in){
		ArrayList<T> list = new ArrayList<>();

		Tuple<Supplier<IForgeRegistry<?>>, Supplier<ResourceKey<? extends Registry<?>>>> ent = REGISTRY_HASH_MAP.getOrDefault(type, null);

		for(Object o : in){
			if(o == null) continue;

			if(o instanceof String tex){
				ResourceLocation location = ResourceLocation.tryParse(tex);
				Optional<? extends Holder<?>> optional = ent.getA().get().getHolder(location);
				optional.ifPresent(s -> list.add((T)s.value()));

				TagKey<T> tagKey = TagKey.create((ResourceKey<? extends Registry<T>>)ent.getB().get(), location);
				if(tagKey.isFor(ent.getA().get().getRegistryKey())){
					ITagManager<T> manager = (ITagManager<T>)ent.getA().get().tags();
					list.addAll((List<T>)manager.getTag(tagKey).stream().toList());
				}
			}
		}

		return list;
	}
}