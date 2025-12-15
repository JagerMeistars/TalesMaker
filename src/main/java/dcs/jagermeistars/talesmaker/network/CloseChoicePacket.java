package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from client to server when player closes a choice window (ESC).
 */
public record CloseChoicePacket(
        ResourceLocation windowId
) implements CustomPacketPayload {

    public static final Type<CloseChoicePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "close_choice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CloseChoicePacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, CloseChoicePacket::windowId,
            CloseChoicePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CloseChoicePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            // Log the close action (for debugging/analytics)
            TalesMaker.LOGGER.debug("Player {} closed choice window: {}",
                    player.getName().getString(), packet.windowId());

            // No server-side action needed for close - the client handles camera restoration
        });
    }
}
