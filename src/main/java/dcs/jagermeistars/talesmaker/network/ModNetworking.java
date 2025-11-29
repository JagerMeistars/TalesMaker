package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = TalesMaker.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TalesMaker.MODID);

        registrar.playToClient(
                NotificationPacket.TYPE,
                NotificationPacket.STREAM_CODEC,
                NotificationPacket::handle
        );

        registrar.playToClient(
                ReloadNotifyPacket.TYPE,
                ReloadNotifyPacket.STREAM_CODEC,
                ReloadNotifyPacket::handle
        );

    }

    public static void sendNotificationToPlayer(ServerPlayer player, String message, int type) {
        PacketDistributor.sendToPlayer(player, new NotificationPacket(message, type));
    }

    public static void sendSuccessToPlayer(ServerPlayer player, String message) {
        sendNotificationToPlayer(player, message, 0);
    }

    public static void sendWarningToPlayer(ServerPlayer player, String message) {
        sendNotificationToPlayer(player, message, 1);
    }

    public static void sendErrorToPlayer(ServerPlayer player, String message) {
        sendNotificationToPlayer(player, message, 2);
    }

    public static void sendToAllPlayers(String message, int type) {
        PacketDistributor.sendToAllPlayers(new NotificationPacket(message, type));
    }

    public static void sendSuccessToAll(String message) {
        sendToAllPlayers(message, 0);
    }

    public static void sendWarningToAll(String message) {
        sendToAllPlayers(message, 1);
    }

    public static void sendErrorToAll(String message) {
        sendToAllPlayers(message, 2);
    }
}
