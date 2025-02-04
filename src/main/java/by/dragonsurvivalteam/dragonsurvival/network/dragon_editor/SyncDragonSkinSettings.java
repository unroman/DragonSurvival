package by.dragonsurvivalteam.dragonsurvival.network.dragon_editor;


import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.config.ConfigHandler;
import by.dragonsurvivalteam.dragonsurvival.network.IMessage;
import by.dragonsurvivalteam.dragonsurvival.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.DistExecutor.SafeRunnable;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;


public class SyncDragonSkinSettings implements IMessage<SyncDragonSkinSettings>{
	public int playerId;
	public boolean newborn;
	public boolean young;
	public boolean adult;

	public SyncDragonSkinSettings(){}

	public SyncDragonSkinSettings(int playerId, boolean newborn, boolean young, boolean adult){
		this.playerId = playerId;
		this.newborn = newborn;
		this.young = young;
		this.adult = adult;
	}

	@Override

	public void encode(SyncDragonSkinSettings message, FriendlyByteBuf buffer){

		buffer.writeInt(message.playerId);
		buffer.writeBoolean(message.newborn);
		buffer.writeBoolean(message.young);
		buffer.writeBoolean(message.adult);
	}

	@Override

	public SyncDragonSkinSettings decode(FriendlyByteBuf buffer){

		int playerId = buffer.readInt();
		boolean newborn = buffer.readBoolean();
		boolean young = buffer.readBoolean();
		boolean adult = buffer.readBoolean();
		return new SyncDragonSkinSettings(playerId, newborn, young, adult);
	}

	@Override
	public void handle(SyncDragonSkinSettings message, Supplier<NetworkEvent.Context> supplier){
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> (SafeRunnable)() -> runClient(message, supplier));

		if(supplier.get().getDirection() == NetworkDirection.PLAY_TO_SERVER){
			ServerPlayer entity = supplier.get().getSender();

			if(entity != null){
				DragonStateProvider.getCap(entity).ifPresent(dragonStateHandler -> {
					dragonStateHandler.getSkinData().renderNewborn = message.newborn;
					dragonStateHandler.getSkinData().renderYoung = message.young;
					dragonStateHandler.getSkinData().renderAdult = message.adult;
				});

				NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), new SyncDragonSkinSettings(entity.getId(), message.newborn, message.young, message.adult));
			}
		}
		supplier.get().setPacketHandled(true);
	}

	@OnlyIn( Dist.CLIENT )
	public void runClient(SyncDragonSkinSettings message, Supplier<NetworkEvent.Context> supplier){
		NetworkEvent.Context context = supplier.get();
		context.enqueueWork(() -> {

			Player thisPlayer = Minecraft.getInstance().player;
			if(thisPlayer != null){
				Level world = thisPlayer.level;
				Entity entity = world.getEntity(message.playerId);
				if(entity instanceof Player){

					DragonStateProvider.getCap(entity).ifPresent(dragonStateHandler -> {
						dragonStateHandler.getSkinData().renderNewborn = message.newborn;
						dragonStateHandler.getSkinData().renderYoung = message.young;
						dragonStateHandler.getSkinData().renderAdult = message.adult;


						if(thisPlayer == entity){
							ConfigHandler.updateConfigValue("renderNewbornSkin", message.newborn);
							ConfigHandler.updateConfigValue("renderYoungSkin", message.young);
							ConfigHandler.updateConfigValue("renderAdultSkin", message.adult);
						}
					});
				}
			}
			context.setPacketHandled(true);
		});
	}
}