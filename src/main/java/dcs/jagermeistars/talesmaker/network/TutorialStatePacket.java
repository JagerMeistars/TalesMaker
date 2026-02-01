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

public record TutorialStatePacket(
        ResourceLocation tutorialId,
        String action
) implements CustomPacketPayload {

    public static final String ACTION_COMPLETE = "complete";

    public static final Type<TutorialStatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "tutorial_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TutorialStatePacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, TutorialStatePacket::tutorialId,
            ByteBufCodecs.STRING_UTF8, TutorialStatePacket::action,
            TutorialStatePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TutorialStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (ACTION_COMPLETE.equalsIgnoreCase(packet.action())) {
                TutorialSessionManager.complete(player);
            }
        });
    }
}
