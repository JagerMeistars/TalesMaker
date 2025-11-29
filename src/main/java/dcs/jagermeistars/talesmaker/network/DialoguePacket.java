package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.client.dialogue.DialogueManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DialoguePacket(
        Component npcName,
        String iconPath,
        Component message
) implements CustomPacketPayload {

    public static final Type<DialoguePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "dialogue"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DialoguePacket> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, DialoguePacket::npcName,
            ByteBufCodecs.STRING_UTF8, DialoguePacket::iconPath,
            ComponentSerialization.STREAM_CODEC, DialoguePacket::message,
            DialoguePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DialoguePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            DialogueManager.showDialogue(
                    packet.npcName(),
                    packet.iconPath().isEmpty() ? null : ResourceLocation.parse(packet.iconPath()),
                    packet.message()
            );
        });
    }
}
