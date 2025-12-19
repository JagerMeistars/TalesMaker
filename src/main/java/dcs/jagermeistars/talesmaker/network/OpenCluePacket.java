package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.client.clue.ClueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Optional;

/**
 * Packet sent from server to client to open a clue inspection screen.
 */
public record OpenCluePacket(
        ResourceLocation presetId,
        Component name,
        Component belonging,      // Can be null
        Component description,
        ResourceLocation model,
        ResourceLocation texture,
        ResourceLocation sound,
        List<ClientClueData> clues,
        String onComplete         // Can be null
) implements CustomPacketPayload {

    /**
     * Represents a clue as sent to the client.
     */
    public record ClientClueData(
            String bone,
            Component name,
            Component description,
            String command  // Can be null
    ) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientClueData> STREAM_CODEC = StreamCodec.of(
                (buf, data) -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, data.bone());
                    ComponentSerialization.STREAM_CODEC.encode(buf, data.name());
                    ComponentSerialization.STREAM_CODEC.encode(buf, data.description());
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buf, Optional.ofNullable(data.command()));
                },
                buf -> new ClientClueData(
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ComponentSerialization.STREAM_CODEC.decode(buf),
                        ComponentSerialization.STREAM_CODEC.decode(buf),
                        ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buf).orElse(null)
                )
        );
    }

    public static final Type<OpenCluePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "open_clue"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCluePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, packet.presetId());
                ComponentSerialization.STREAM_CODEC.encode(buf, packet.name());
                ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC).encode(buf, Optional.ofNullable(packet.belonging()));
                ComponentSerialization.STREAM_CODEC.encode(buf, packet.description());
                ResourceLocation.STREAM_CODEC.encode(buf, packet.model());
                ResourceLocation.STREAM_CODEC.encode(buf, packet.texture());
                ResourceLocation.STREAM_CODEC.encode(buf, packet.sound());
                ClientClueData.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, packet.clues());
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buf, Optional.ofNullable(packet.onComplete()));
            },
            buf -> new OpenCluePacket(
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    ComponentSerialization.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC).decode(buf).orElse(null),
                    ComponentSerialization.STREAM_CODEC.decode(buf),
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    ClientClueData.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buf).orElse(null)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenCluePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            // Open clue inspection screen
            mc.setScreen(new ClueScreen(
                    packet.presetId(),
                    packet.name(),
                    packet.belonging(),
                    packet.description(),
                    packet.model(),
                    packet.texture(),
                    packet.sound(),
                    packet.clues(),
                    packet.onComplete()
            ));
        });
    }
}
