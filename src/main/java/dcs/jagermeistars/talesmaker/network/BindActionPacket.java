package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.client.bind.BindManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BindActionPacket(String action, String key, String command) implements CustomPacketPayload {

    public static final Type<BindActionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "bind_action"));

    public static final StreamCodec<FriendlyByteBuf, BindActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BindActionPacket::action,
            ByteBufCodecs.STRING_UTF8, BindActionPacket::key,
            ByteBufCodecs.STRING_UTF8, BindActionPacket::command,
            BindActionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BindActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            BindManager.load();
            String action = packet.action();
            if ("add".equalsIgnoreCase(action)) {
                BindManager.BindAddResult result = BindManager.addBind(packet.key(), packet.command());
                if (result == BindManager.BindAddResult.OK) {
                    BindManager.sendMessage("Bind added for " + packet.key(), false);
                } else if (result == BindManager.BindAddResult.USED) {
                    BindManager.sendMessage("Key already in use: " + packet.key(), true);
                } else {
                    BindManager.sendMessage("Unknown key: " + packet.key(), true);
                }
            } else if ("remove".equalsIgnoreCase(action)) {
                boolean ok = BindManager.removeBind(packet.key());
                if (ok) {
                    BindManager.sendMessage("Bind removed for " + packet.key(), false);
                } else {
                    BindManager.sendMessage("No bind found for " + packet.key(), true);
                }
            }
        });
    }
}
