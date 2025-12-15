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

        registrar.playToClient(
                DialoguePacket.TYPE,
                DialoguePacket.STREAM_CODEC,
                DialoguePacket::handle
        );

        registrar.playToClient(
                DialogueTimesPacket.TYPE,
                DialogueTimesPacket.STREAM_CODEC,
                DialogueTimesPacket::handle
        );

        registrar.playToClient(
                ClearHistoryPacket.TYPE,
                ClearHistoryPacket.STREAM_CODEC,
                ClearHistoryPacket::handle
        );

        registrar.playToServer(
                InteractScriptPacket.TYPE,
                InteractScriptPacket.STREAM_CODEC,
                InteractScriptPacket::handle
        );

        // Choice system packets
        registrar.playToClient(
                OpenChoicePacket.TYPE,
                OpenChoicePacket.STREAM_CODEC,
                OpenChoicePacket::handle
        );

        registrar.playToServer(
                SelectChoicePacket.TYPE,
                SelectChoicePacket.STREAM_CODEC,
                SelectChoicePacket::handle
        );

        registrar.playToServer(
                CloseChoicePacket.TYPE,
                CloseChoicePacket.STREAM_CODEC,
                CloseChoicePacket::handle
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
