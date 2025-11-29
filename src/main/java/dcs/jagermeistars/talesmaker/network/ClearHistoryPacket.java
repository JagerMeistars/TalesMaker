package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.client.dialogue.DialogueHistory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClearHistoryPacket() implements CustomPacketPayload {

    public static final Type<ClearHistoryPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "clear_history"));

    public static final StreamCodec<FriendlyByteBuf, ClearHistoryPacket> STREAM_CODEC = StreamCodec.unit(new ClearHistoryPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClearHistoryPacket packet, IPayloadContext context) {
        context.enqueueWork(DialogueHistory::clear);
    }
}
