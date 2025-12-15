package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.data.choice.Choice;
import dcs.jagermeistars.talesmaker.data.choice.ChoiceWindow;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from client to server when player selects a choice.
 */
public record SelectChoicePacket(
        ResourceLocation windowId,
        int choiceIndex,
        int speakerEntityId
) implements CustomPacketPayload {

    public static final Type<SelectChoicePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "select_choice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectChoicePacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SelectChoicePacket::windowId,
            ByteBufCodecs.INT, SelectChoicePacket::choiceIndex,
            ByteBufCodecs.INT, SelectChoicePacket::speakerEntityId,
            SelectChoicePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectChoicePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            // Get the choice window
            ChoiceWindow window = TalesMaker.CHOICE_MANAGER.getWindow(packet.windowId()).orElse(null);
            if (window == null) {
                TalesMaker.LOGGER.warn("Player {} tried to select choice from unknown window: {}",
                        player.getName().getString(), packet.windowId());
                return;
            }

            // Validate choice index
            if (packet.choiceIndex() < 0 || packet.choiceIndex() >= window.choices().size()) {
                TalesMaker.LOGGER.warn("Player {} sent invalid choice index {} for window {}",
                        player.getName().getString(), packet.choiceIndex(), packet.windowId());
                return;
            }

            Choice choice = window.choices().get(packet.choiceIndex());

            // Re-check conditions on server side
            if (choice.isHidden()) {
                Choice.HiddenCondition hidden = choice.hidden().get();
                if (!evaluateCondition(player, hidden.predicate().orElse(null), hidden.advancement().orElse(null))) {
                    TalesMaker.LOGGER.warn("Player {} tried to select hidden choice in window {}",
                            player.getName().getString(), packet.windowId());
                    return;
                }
            }

            if (choice.isLockable()) {
                Choice.LockedCondition locked = choice.locked().get();
                if (!evaluateCondition(player, locked.predicate().orElse(null), locked.advancement().orElse(null))) {
                    TalesMaker.LOGGER.warn("Player {} tried to select locked choice in window {}",
                            player.getName().getString(), packet.windowId());
                    return;
                }
            }

            // Execute the command
            String command = choice.command();
            if (command != null && !command.isEmpty()) {
                // Get speaker entity for placeholder replacement
                Entity speaker = player.level().getEntity(packet.speakerEntityId());

                // Replace placeholders
                command = command
                        .replace("{player}", player.getName().getString())
                        .replace("{x}", String.valueOf((int) player.getX()))
                        .replace("{y}", String.valueOf((int) player.getY()))
                        .replace("{z}", String.valueOf((int) player.getZ()));

                if (speaker != null) {
                    command = command
                            .replace("{speaker}", speaker.getName().getString())
                            .replace("{speaker_x}", String.valueOf((int) speaker.getX()))
                            .replace("{speaker_y}", String.valueOf((int) speaker.getY()))
                            .replace("{speaker_z}", String.valueOf((int) speaker.getZ()));
                }

                // Execute command from server console
                try {
                    player.getServer().getCommands().performPrefixedCommand(
                            player.getServer().createCommandSourceStack(),
                            command
                    );
                } catch (Exception e) {
                    TalesMaker.LOGGER.error("Failed to execute choice command for player {}: {}",
                            player.getName().getString(), command, e);
                }
            }
        });
    }

    /**
     * Evaluate a predicate or advancement condition for a player.
     */
    private static boolean evaluateCondition(ServerPlayer player, ResourceLocation predicate, ResourceLocation advancement) {
        if (predicate != null) {
            var lootData = player.server.reloadableRegistries().get();
            var predicateHolder = lootData.lookup(net.minecraft.core.registries.Registries.PREDICATE)
                    .flatMap(registry -> registry.get(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.PREDICATE, predicate)));

            if (predicateHolder.isPresent()) {
                var lootContext = new net.minecraft.world.level.storage.loot.LootContext.Builder(
                        new net.minecraft.world.level.storage.loot.LootParams.Builder(player.serverLevel())
                                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, player)
                                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, player.position())
                                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND)
                ).create(java.util.Optional.empty());

                return predicateHolder.get().value().test(lootContext);
            }
            return false;
        }

        if (advancement != null) {
            var advancementHolder = player.server.getAdvancements().get(advancement);
            if (advancementHolder != null) {
                return player.getAdvancements().getOrStartProgress(advancementHolder).isDone();
            }
            return false;
        }

        return true; // No condition means always true
    }
}
