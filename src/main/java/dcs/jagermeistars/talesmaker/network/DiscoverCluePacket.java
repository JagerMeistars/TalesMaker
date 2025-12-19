package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.data.clue.CluePreset;
import dcs.jagermeistars.talesmaker.server.ClueSessionManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from client to server when player discovers a clue.
 */
public record DiscoverCluePacket(
        ResourceLocation presetId,
        String clueId  // The bone name of the discovered clue
) implements CustomPacketPayload {

    public static final Type<DiscoverCluePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "discover_clue"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiscoverCluePacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, DiscoverCluePacket::presetId,
            ByteBufCodecs.STRING_UTF8, DiscoverCluePacket::clueId,
            DiscoverCluePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DiscoverCluePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            // Get the clue preset
            CluePreset preset = TalesMaker.CLUE_MANAGER.getPreset(packet.presetId()).orElse(null);
            if (preset == null) {
                TalesMaker.LOGGER.warn("Player {} tried to discover clue from unknown preset: {}",
                        player.getName().getString(), packet.presetId());
                return;
            }

            // Find the clue data by bone name
            CluePreset.ClueData clueData = null;
            for (CluePreset.ClueData data : preset.clues()) {
                if (data.bone().equals(packet.clueId())) {
                    clueData = data;
                    break;
                }
            }

            if (clueData == null) {
                TalesMaker.LOGGER.warn("Player {} tried to discover unknown clue '{}' in preset {}",
                        player.getName().getString(), packet.clueId(), packet.presetId());
                return;
            }

            // Register discovery with session manager
            boolean alreadyDiscovered = !ClueSessionManager.discoverClue(player, packet.clueId());
            if (alreadyDiscovered) {
                // Already discovered, ignore
                return;
            }

            // Play discovery sound
            ResourceLocation soundId = preset.getDiscoverySound();
            player.level().playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(soundId),
                    SoundSource.PLAYERS,
                    1.0f, 1.0f
            );

            // Execute clue command if present
            if (clueData.command().isPresent()) {
                String command = clueData.command().get();
                try {
                    player.getServer().getCommands().performPrefixedCommand(
                            player.getServer().createCommandSourceStack()
                                    .withEntity(player)
                                    .withPosition(player.position()),
                            command
                    );
                } catch (Exception e) {
                    TalesMaker.LOGGER.error("Failed to execute clue command for player {}: {}",
                            player.getName().getString(), command, e);
                }
            }

            // Check if all clues found and execute on_complete
            if (ClueSessionManager.isComplete(player)) {
                if (preset.onComplete().isPresent()) {
                    String onCompleteCommand = preset.onComplete().get();
                    try {
                        player.getServer().getCommands().performPrefixedCommand(
                                player.getServer().createCommandSourceStack()
                                        .withEntity(player)
                                        .withPosition(player.position()),
                                onCompleteCommand
                        );
                    } catch (Exception e) {
                        TalesMaker.LOGGER.error("Failed to execute on_complete command for player {}: {}",
                                player.getName().getString(), onCompleteCommand, e);
                    }
                }
            }
        });
    }
}
