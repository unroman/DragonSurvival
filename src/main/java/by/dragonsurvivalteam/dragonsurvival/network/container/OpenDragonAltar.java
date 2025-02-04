package by.dragonsurvivalteam.dragonsurvival.network.container;

import by.dragonsurvivalteam.dragonsurvival.client.handlers.DragonAltarHandler;
import by.dragonsurvivalteam.dragonsurvival.network.IMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.DistExecutor.SafeRunnable;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenDragonAltar implements IMessage<OpenDragonAltar>{

	public OpenDragonAltar(){}

	@Override
	public void encode(OpenDragonAltar message, FriendlyByteBuf buffer){

	}

	@Override
	public OpenDragonAltar decode(FriendlyByteBuf buffer){
		return new OpenDragonAltar();
	}

	@Override
	public void handle(OpenDragonAltar message, Supplier<NetworkEvent.Context> supplier){
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> (SafeRunnable)() -> runClient(message, supplier));
		supplier.get().setPacketHandled(true);
	}

	@OnlyIn( Dist.CLIENT )
	public void runClient(OpenDragonAltar message, Supplier<NetworkEvent.Context> supplier){
		NetworkEvent.Context context = supplier.get();
		context.enqueueWork(() -> {
			DragonAltarHandler.OpenAltar();
			context.setPacketHandled(true);
		});
	}
}