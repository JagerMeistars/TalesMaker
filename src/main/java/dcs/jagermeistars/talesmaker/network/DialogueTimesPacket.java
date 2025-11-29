package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.client.dialogue.DialogueManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DialogueTimesPacket(int durationTicks) implements CustomPacketPayload {

    public static final Type<DialogueTimesPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "dialogue_times"));

    public static final StreamCodec<FriendlyByteBuf, DialogueTimesPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, DialogueTimesPacket::durationTicks,
            DialogueTimesPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DialogueTimesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            DialogueManager.setDuration(packet.durationTicks());
        });
    }
}
