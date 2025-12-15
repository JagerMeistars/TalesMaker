package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.client.choice.ChoiceCameraController;
import dcs.jagermeistars.talesmaker.client.choice.ChoiceScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Packet sent from server to client to open a choice window.
 */
public record OpenChoicePacket(
        ResourceLocation windowId,
        int speakerEntityId,
        Component speakerName,
        Component dialogueText,
        List<ClientChoice> choices,
        String mode,
        int timer,
        int timeoutChoice
) implements CustomPacketPayload {

    /**
     * Represents a choice as sent to the client (pre-evaluated conditions).
     */
    public record ClientChoice(
            Component text,
            boolean locked,
            Component lockedMessage
    ) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientChoice> STREAM_CODEC = StreamCodec.composite(
                ComponentSerialization.STREAM_CODEC, ClientChoice::text,
                ByteBufCodecs.BOOL, ClientChoice::locked,
                ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs::optional), ClientChoice::lockedMessageOptional,
                (text, locked, lockedMessageOpt) -> new ClientChoice(text, locked, lockedMessageOpt.orElse(null))
        );

        private java.util.Optional<Component> lockedMessageOptional() {
            return java.util.Optional.ofNullable(lockedMessage);
        }
    }

    public static final Type<OpenChoicePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "open_choice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenChoicePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, packet.windowId());
                ByteBufCodecs.INT.encode(buf, packet.speakerEntityId());
                ComponentSerialization.STREAM_CODEC.encode(buf, packet.speakerName());
                ComponentSerialization.STREAM_CODEC.encode(buf, packet.dialogueText());
                ClientChoice.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, packet.choices());
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.mode());
                ByteBufCodecs.INT.encode(buf, packet.timer());
                ByteBufCodecs.INT.encode(buf, packet.timeoutChoice());
            },
            buf -> new OpenChoicePacket(
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ComponentSerialization.STREAM_CODEC.decode(buf),
                    ComponentSerialization.STREAM_CODEC.decode(buf),
                    ClientChoice.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenChoicePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Entity speaker = mc.level.getEntity(packet.speakerEntityId());
            if (speaker == null) {
                TalesMaker.LOGGER.warn("Choice packet received but speaker entity {} not found", packet.speakerEntityId());
                return;
            }

            // Play choice screen open sound
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0f, 1.0f);
            }

            // Start cinematic camera
            ChoiceCameraController.startCinematic(speaker);

            // Open choice screen
            mc.setScreen(new ChoiceScreen(
                    packet.windowId(),
                    speaker,
                    packet.speakerName(),
                    packet.dialogueText(),
                    packet.choices(),
                    packet.mode(),
                    packet.timer(),
                    packet.timeoutChoice()
            ));
        });
    }
}
