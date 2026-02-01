package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.tutorial.TutorialSessionManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TutorialStepStatePacket(
        ResourceLocation tutorialId,
        boolean allowInteraction
) implements CustomPacketPayload {

    public static final Type<TutorialStepStatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "tutorial_step_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TutorialStepStatePacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, TutorialStepStatePacket::tutorialId,
            ByteBufCodecs.BOOL, TutorialStepStatePacket::allowInteraction,
            TutorialStepStatePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TutorialStepStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            TutorialSessionManager.setAllowInteraction(player, packet.allowInteraction());
        });
    }
}
