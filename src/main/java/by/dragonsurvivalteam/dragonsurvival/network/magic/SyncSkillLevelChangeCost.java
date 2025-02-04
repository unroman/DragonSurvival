package by.dragonsurvivalteam.dragonsurvival.network.magic;

import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.magic.DragonAbilities;
import by.dragonsurvivalteam.dragonsurvival.magic.common.DragonAbility;
import by.dragonsurvivalteam.dragonsurvival.magic.common.passive.PassiveDragonAbility;
import by.dragonsurvivalteam.dragonsurvival.network.IMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Synchronizes the logic of consuming or giving experience to the server side to prevent de-syncs
 */
public class SyncSkillLevelChangeCost implements IMessage<SyncSkillLevelChangeCost>{
	private int level;
	private int levelChange;
	private String skill;

	public SyncSkillLevelChangeCost(int level, String skill, int levelChange){
		this.level = level;
		this.skill = skill;
		this.levelChange = levelChange;
	}

	public SyncSkillLevelChangeCost(){}

	@Override
	public void encode(SyncSkillLevelChangeCost message, FriendlyByteBuf buffer){
		buffer.writeInt(message.level);
		buffer.writeUtf(message.skill);
		buffer.writeInt(message.levelChange);
	}

	@Override
	public SyncSkillLevelChangeCost decode(FriendlyByteBuf buffer){
		int level = buffer.readInt();
		String skill = buffer.readUtf();
		int levelChange = buffer.readInt();
		return new SyncSkillLevelChangeCost(level, skill, levelChange);
	}

	@Override
	public void handle(final SyncSkillLevelChangeCost message, final Supplier<NetworkEvent.Context> supplier){
		ServerPlayer player = supplier.get().getSender();

		if (player == null) {
			supplier.get().setPacketHandled(true);
			return;
		}

		supplier.get().enqueueWork(() -> DragonStateProvider.getCap(player).ifPresent(dragonStateHandler -> {
			DragonAbility staticAbility = DragonAbilities.ABILITY_LOOKUP.get(message.skill);

			if (staticAbility instanceof PassiveDragonAbility ability) {
				PassiveDragonAbility playerAbility = DragonAbilities.getSelfAbility(player, ability.getClass());
				int levelCost = message.levelChange > 0 ? -playerAbility.getLevelCost(message.levelChange) : Math.max((int) (playerAbility.getLevelCost() * 0.8F), 1);

				if (levelCost != 0 && !player.isCreative()) {
					player.giveExperienceLevels(levelCost);
				}
			}
		}));

		supplier.get().setPacketHandled(true);
	}
}