package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Client -> Server: Response with list of missing resources for a preset
 */
public record ResourceValidationResponsePacket(
        String presetId,
        List<String> missingResources
) implements CustomPacketPayload {

    public static final Type<ResourceValidationResponsePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "resource_validation_response"));

    public static final StreamCodec<FriendlyByteBuf, ResourceValidationResponsePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ResourceValidationResponsePacket::presetId,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ResourceValidationResponsePacket::missingResources,
            ResourceValidationResponsePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResourceValidationResponsePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.missingResources().isEmpty()) {
                return;
            }

            // Get player who sent this response
            if (context.player() instanceof ServerPlayer player) {
                // Send warning notifications for each missing resource
                for (String missing : packet.missingResources()) {
                    ModNetworking.sendWarningToPlayer(player, "Missing " + missing);
                }
            }
        });
    }
}
