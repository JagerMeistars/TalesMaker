package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.client.notification.NotificationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record NotificationPacket(String message, int notificationType) implements CustomPacketPayload {

    public static final Type<NotificationPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "notification"));

    public static final StreamCodec<FriendlyByteBuf, NotificationPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, NotificationPacket::message,
            ByteBufCodecs.INT, NotificationPacket::notificationType,
            NotificationPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NotificationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            NotificationManager.NotificationType notifType = switch (packet.notificationType()) {
                case 0 -> NotificationManager.NotificationType.SUCCESS;
                case 1 -> NotificationManager.NotificationType.WARNING;
                default -> NotificationManager.NotificationType.ERROR;
            };
            NotificationManager.addNotification(packet.message(), notifType);
        });
    }
}
