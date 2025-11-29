package dcs.jagermeistars.talesmaker.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.data.NpcPreset;
import dcs.jagermeistars.talesmaker.entity.NpcEntity;
import dcs.jagermeistars.talesmaker.init.ModEntities;
import dcs.jagermeistars.talesmaker.monologue.MonologueManager;
import dcs.jagermeistars.talesmaker.network.ClearHistoryPacket;
import dcs.jagermeistars.talesmaker.network.DialoguePacket;
import dcs.jagermeistars.talesmaker.network.DialogueTimesPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class TalesMakerCommands {

    private static final SuggestionProvider<CommandSourceStack> PRESET_SUGGESTIONS = (context, builder) -> {
        // Get preset IDs from the preset manager
        return SharedSuggestionProvider.suggest(
                TalesMaker.PRESET_MANAGER.getAllPresets().keySet().stream()
                        .map(ResourceLocation::toString),
                builder);
    };

    private static final SuggestionProvider<CommandSourceStack> NPC_ID_SUGGESTIONS = (context, builder) -> {
        // Get all custom NPC IDs from the world
        if (context.getSource().getLevel() instanceof ServerLevel serverLevel) {
            return SharedSuggestionProvider.suggest(
                    serverLevel.getEntities(ModEntities.NPC.get(), npc -> !npc.getCustomId().isEmpty())
                            .stream()
                            .map(NpcEntity::getCustomId)
                            .distinct(),
                    builder);
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(
                Commands.literal("talesmaker")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("npc")
                                .then(Commands.literal("create")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .then(Commands.argument("preset", ResourceLocationArgument.id())
                                                        .suggests(PRESET_SUGGESTIONS)
                                                        .executes(TalesMakerCommands::createNpc)))))
                        .then(Commands.literal("dialogue")
                                // /talesmaker dialogue times <duration>
                                .then(Commands.literal("times")
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                                .executes(TalesMakerCommands::dialogueTimes)))
                                // /talesmaker dialogue say <npc_id> <message>
                                .then(Commands.literal("say")
                                        .then(Commands.argument("npc_id", StringArgumentType.word())
                                                .suggests(NPC_ID_SUGGESTIONS)
                                                .then(Commands.argument("message", ComponentArgument.textComponent(buildContext))
                                                        .executes(TalesMakerCommands::dialogueById)))
                                        // /talesmaker dialogue say @e[...] <message>
                                        .then(Commands.argument("targets", EntityArgument.entities())
                                                .then(Commands.argument("message", ComponentArgument.textComponent(buildContext))
                                                        .executes(TalesMakerCommands::dialogueBySelector)))))
                        .then(Commands.literal("monologue")
                                // /talesmaker monologue enable <preset>
                                .then(Commands.literal("enable")
                                        .then(Commands.argument("preset", ResourceLocationArgument.id())
                                                .suggests(PRESET_SUGGESTIONS)
                                                .executes(TalesMakerCommands::monologueEnable)))
                                // /talesmaker monologue disable
                                .then(Commands.literal("disable")
                                        .executes(TalesMakerCommands::monologueDisable)))
                        .then(Commands.literal("history")
                                // /talesmaker history clear
                                .then(Commands.literal("clear")
                                        .executes(TalesMakerCommands::historyClear))));
    }

    private static int createNpc(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String customId = StringArgumentType.getString(context, "id");
        ResourceLocation presetId = ResourceLocationArgument.getId(context, "preset");

        if (!(source.getLevel() instanceof ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("This command can only be used in a server world"));
            return 0;
        }

        // Check if NPC with this ID already exists
        boolean idExists = serverLevel.getEntities(ModEntities.NPC.get(), npc -> customId.equals(npc.getCustomId()))
                .stream().findAny().isPresent();
        if (idExists) {
            source.sendFailure(Component.literal("NPC with id '" + customId + "' already exists"));
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

        // Apply preset and custom ID
        npc.setCustomId(customId);
        npc.setPreset(preset);

        // Spawn in world
        serverLevel.addFreshEntity(npc);

        source.sendSuccess(() -> Component.literal("Created NPC '" + customId + "' with preset: ").append(preset.name()), true);
        return 1;
    }

    private static int dialogueById(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "npc_id");
        Component message = ComponentArgument.getComponent(context, "message");

        if (!(source.getLevel() instanceof ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("This command can only be used in a server world"));
            return 0;
        }

        // Find NPC by custom ID
        NpcEntity npc = serverLevel.getEntities(ModEntities.NPC.get(), entity -> npcId.equals(entity.getCustomId()))
                .stream().findFirst().orElse(null);

        if (npc == null) {
            source.sendFailure(Component.literal("NPC with id '" + npcId + "' not found"));
            return 0;
        }

        return sendDialogue(source, npc, message);
    }

    private static int dialogueBySelector(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var entities = EntityArgument.getEntities(context, "targets");
        Component message = ComponentArgument.getComponent(context, "message");

        NpcEntity npc = null;
        for (Entity entity : entities) {
            if (entity instanceof NpcEntity npcEntity) {
                npc = npcEntity;
                break;
            }
        }

        if (npc == null) {
            source.sendFailure(Component.literal("No NPC found in selection"));
            return 0;
        }

        return sendDialogue(source, npc, message);
    }

    private static int sendDialogue(CommandSourceStack source, NpcEntity npc, Component message) {
        // Get icon from preset - icon is required
        ResourceLocation presetId = npc.getPresetResourceLocation();
        if (presetId == null) {
            source.sendFailure(Component.literal("NPC has no preset assigned"));
            return 0;
        }

        NpcPreset preset = TalesMaker.PRESET_MANAGER.getPreset(presetId).orElse(null);
        if (preset == null) {
            source.sendFailure(Component.literal("NPC preset not found: " + presetId));
            return 0;
        }

        if (preset.icon() == null) {
            source.sendFailure(Component.literal("NPC preset '" + presetId + "' has no icon defined"));
            return 0;
        }

        String iconPath = preset.icon().toString();

        // Create and send packet to all players
        DialoguePacket packet = new DialoguePacket(
                npc.getNpcName(),
                iconPath,
                message
        );

        PacketDistributor.sendToAllPlayers(packet);

        source.sendSuccess(() -> Component.literal("Sent dialogue from NPC"), true);
        return 1;
    }

    private static int dialogueTimes(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int duration = IntegerArgumentType.getInteger(context, "duration");

        // Send to all players
        PacketDistributor.sendToAllPlayers(new DialogueTimesPacket(duration));

        source.sendSuccess(() -> Component.literal("Set dialogue duration to " + duration + " ticks"), true);
        return 1;
    }

    private static int monologueEnable(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ResourceLocation presetId = ResourceLocationArgument.getId(context, "preset");

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be executed by a player"));
            return 0;
        }

        NpcPreset preset = TalesMaker.PRESET_MANAGER.getPreset(presetId).orElse(null);
        if (preset == null) {
            source.sendFailure(Component.literal("Unknown preset: " + presetId));
            return 0;
        }

        if (preset.icon() == null) {
            source.sendFailure(Component.literal("Preset '" + presetId + "' has no icon defined"));
            return 0;
        }

        MonologueManager.enableMonologue(player.getUUID(), presetId);
        source.sendSuccess(() -> Component.literal("Monologue enabled with preset: ").append(preset.name()), true);
        return 1;
    }

    private static int monologueDisable(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be executed by a player"));
            return 0;
        }

        if (!MonologueManager.hasMonologue(player.getUUID())) {
            source.sendFailure(Component.literal("Monologue is not enabled"));
            return 0;
        }

        MonologueManager.disableMonologue(player.getUUID());
        source.sendSuccess(() -> Component.literal("Monologue disabled"), true);
        return 1;
    }

    private static int historyClear(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        // Send clear packet to all players
        PacketDistributor.sendToAllPlayers(new ClearHistoryPacket());

        source.sendSuccess(() -> Component.literal("Dialogue history cleared for all players"), true);
        return 1;
    }
}
