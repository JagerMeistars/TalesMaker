package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.entity.NpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record InteractScriptPacket(int entityId) implements CustomPacketPayload {

    public static final Type<InteractScriptPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "interact_script"));

    public static final StreamCodec<FriendlyByteBuf, InteractScriptPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, InteractScriptPacket::entityId,
            InteractScriptPacket::new);

    private static final double MAX_INTERACT_DISTANCE = 6.0;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(InteractScriptPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            // Null check for level
            if (serverPlayer.level() == null) {
                return;
            }

            Entity entity = serverPlayer.level().getEntity(packet.entityId());
            if (!(entity instanceof NpcEntity npc)) {
                return;
            }

            // Check distance
            if (serverPlayer.distanceTo(npc) > MAX_INTERACT_DISTANCE) {
                return;
            }

            // Check if NPC has interact script
            if (!npc.hasInteractScript()) {
                return;
            }

            // Check if already used
            if (npc.isInteractUsed()) {
                return;
            }

            // Mark as used BEFORE executing command to prevent race condition
            npc.setInteractUsed(true);

            // Execute script
            String command = npc.getScriptCommand();
            command = replacePlaceholders(command, serverPlayer, npc);

            // Execute command from server console with error handling
            try {
                serverPlayer.server.getCommands().performPrefixedCommand(
                        serverPlayer.server.createCommandSourceStack(),
                        command
                );
            } catch (Exception e) {
                TalesMaker.LOGGER.error("Failed to execute interact script: {}", command, e);
            }
        });
    }

    private static String replacePlaceholders(String command, ServerPlayer player, NpcEntity npc) {
        return command
                .replace("{player}", player.getName().getString())
                .replace("{npc}", npc.getCustomId())
                .replace("{x}", String.valueOf((int) npc.getX()))
                .replace("{y}", String.valueOf((int) npc.getY()))
                .replace("{z}", String.valueOf((int) npc.getZ()));
    }
}
