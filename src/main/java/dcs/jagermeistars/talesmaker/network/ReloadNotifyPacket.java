package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.client.animation.AnimationValidator;
import dcs.jagermeistars.talesmaker.client.model.NpcModel;
import dcs.jagermeistars.talesmaker.client.notification.ResourceErrorManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ReloadNotifyPacket(int presetCount, boolean hasErrors) implements CustomPacketPayload {

    public static final Type<ReloadNotifyPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "reload_notify"));

    public static final StreamCodec<FriendlyByteBuf, ReloadNotifyPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ReloadNotifyPacket::presetCount,
            ByteBufCodecs.BOOL, ReloadNotifyPacket::hasErrors,
            ReloadNotifyPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReloadNotifyPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Clear validation cache so errors will be shown again on next render
            NpcModel.clearValidationCache();
            AnimationValidator.clearCache();
            // Clear resource error cache to allow new errors to be displayed
            ResourceErrorManager.clearCache();
        });
    }
}
