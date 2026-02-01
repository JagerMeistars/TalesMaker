package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.client.tutorial.TutorialClientManager;
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

public record TutorialStartPacket(
        ResourceLocation tutorialId,
        String tutorialType,
        Component title,
        Component description,
        List<ClientStep> steps,
        Optional<ResourceLocation> soundOpen,
        Optional<ResourceLocation> soundClose,
        Optional<ResourceLocation> soundAdvance
) implements CustomPacketPayload {

    public record ClientStep(
            Component text,
            float cutoutX,
            float cutoutY,
            float cutoutWidth,
            float cutoutHeight,
            String advanceKey,
            String openScreen,
            String command,
            boolean allowInteraction
    ) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientStep> STREAM_CODEC = StreamCodec.of(
                (buf, step) -> {
                    ComponentSerialization.STREAM_CODEC.encode(buf, step.text());
                    ByteBufCodecs.FLOAT.encode(buf, step.cutoutX());
                    ByteBufCodecs.FLOAT.encode(buf, step.cutoutY());
                    ByteBufCodecs.FLOAT.encode(buf, step.cutoutWidth());
                    ByteBufCodecs.FLOAT.encode(buf, step.cutoutHeight());
                    ByteBufCodecs.STRING_UTF8.encode(buf, step.advanceKey());
                    ByteBufCodecs.STRING_UTF8.encode(buf, step.openScreen());
                    ByteBufCodecs.STRING_UTF8.encode(buf, step.command());
                    ByteBufCodecs.BOOL.encode(buf, step.allowInteraction());
                },
                buf -> new ClientStep(
                        ComponentSerialization.STREAM_CODEC.decode(buf),
                        ByteBufCodecs.FLOAT.decode(buf),
                        ByteBufCodecs.FLOAT.decode(buf),
                        ByteBufCodecs.FLOAT.decode(buf),
                        ByteBufCodecs.FLOAT.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.BOOL.decode(buf)
                )
        );
    }

    public static final Type<TutorialStartPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "tutorial_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TutorialStartPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, packet.tutorialId());
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.tutorialType());
                ComponentSerialization.STREAM_CODEC.encode(buf, packet.title());
                ComponentSerialization.STREAM_CODEC.encode(buf, packet.description());
                ClientStep.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, packet.steps());
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional).encode(buf, packet.soundOpen());
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional).encode(buf, packet.soundClose());
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional).encode(buf, packet.soundAdvance());
            },
            buf -> new TutorialStartPacket(
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ComponentSerialization.STREAM_CODEC.decode(buf),
                    ComponentSerialization.STREAM_CODEC.decode(buf),
                    ClientStep.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional).decode(buf),
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional).decode(buf),
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional).decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TutorialStartPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> TutorialClientManager.startTutorial(packet));
    }
}
