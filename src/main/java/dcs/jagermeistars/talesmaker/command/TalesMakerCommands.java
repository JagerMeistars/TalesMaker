package dcs.jagermeistars.talesmaker.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.data.NpcPreset;
import dcs.jagermeistars.talesmaker.entity.NpcEntity;
import dcs.jagermeistars.talesmaker.init.ModEntities;
import dcs.jagermeistars.talesmaker.network.ValidateResourcesPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class TalesMakerCommands {

    private static final SuggestionProvider<CommandSourceStack> PRESET_SUGGESTIONS = (context, builder) -> {
        // Get preset IDs from the preset manager
        return SharedSuggestionProvider.suggest(
                TalesMaker.PRESET_MANAGER.getAllPresets().keySet().stream()
                        .map(ResourceLocation::toString),
                builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("talesmaker")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("npc")
                                .then(Commands.literal("create")
                                        .then(Commands.argument("preset", ResourceLocationArgument.id())
                                                .suggests(PRESET_SUGGESTIONS)
                                                .executes(TalesMakerCommands::createNpc)))));
    }

    private static int createNpc(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ResourceLocation presetId = ResourceLocationArgument.getId(context, "preset");

        if (!(source.getLevel() instanceof ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("This command can only be used in a server world"));
            return 0;
        }

        Vec3 position = source.getPosition();

        // Load preset from manager
        NpcPreset preset = TalesMaker.PRESET_MANAGER.getPreset(presetId).orElse(null);

        if (preset == null) {
            source.sendFailure(Component.literal("Unknown preset: " + presetId));
            return 0;
        }

        // Create NPC entity
        NpcEntity npc = ModEntities.NPC.get().create(serverLevel);
        if (npc == null) {
            source.sendFailure(Component.literal("Failed to create NPC entity"));
            return 0;
        }

        // Set position
        npc.moveTo(position.x, position.y, position.z, source.getRotation().y, 0);

        // Apply preset
        npc.setPreset(preset);

        // Spawn in world
        serverLevel.addFreshEntity(npc);

        // Request resource validation from all clients
        PacketDistributor.sendToAllPlayers(new ValidateResourcesPacket(
                presetId.toString(),
                preset.model().toString(),
                preset.texture().toString(),
                preset.emissive() != null ? preset.emissive().toString() : "",
                preset.animations().path().toString()
        ));

        source.sendSuccess(() -> Component.literal("Created NPC with preset: ").append(preset.name()), true);
        return 1;
    }
}
