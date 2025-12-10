package dcs.jagermeistars.talesmaker.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
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
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
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

    private static final SuggestionProvider<CommandSourceStack> SCRIPT_TYPE_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(new String[]{"default", "interact", "player_nearby"}, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> BOOLEAN_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(new String[]{"true", "false"}, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> LOOK_DURATION_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(new String[]{"once", "continuous"}, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> ANIM_MODE_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(new String[]{"once", "loop", "hold"}, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(
                Commands.literal("talesmaker")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("npc")
                                // /talesmaker npc create <pos> <id> <preset> [invulnerable] [script_type] [command]
                                .then(Commands.literal("create")
                                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .then(Commands.argument("preset", ResourceLocationArgument.id())
                                                                .suggests(PRESET_SUGGESTIONS)
                                                                .executes(TalesMakerCommands::createNpcAtPos)
                                                                // Path with invulnerable
                                                                .then(Commands.argument("invulnerable", BoolArgumentType.bool())
                                                                        .executes(TalesMakerCommands::createNpcWithInvulnerable)
                                                                        .then(Commands.argument("script_type", StringArgumentType.word())
                                                                                .suggests(SCRIPT_TYPE_SUGGESTIONS)
                                                                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                                                                        .executes(TalesMakerCommands::createNpcFullWithInvulnerable))))))))
                                // /talesmaker npc remove <id>
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests(NPC_ID_SUGGESTIONS)
                                                .executes(TalesMakerCommands::removeNpc)))
                                // /talesmaker npc set ...
                                .then(Commands.literal("set")
                                        .then(Commands.literal("script")
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .suggests(NPC_ID_SUGGESTIONS)
                                                        .then(Commands.argument("script_type", StringArgumentType.word())
                                                                .suggests(SCRIPT_TYPE_SUGGESTIONS)
                                                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                                                        .executes(TalesMakerCommands::setNpcScript)))))
                                        .then(Commands.literal("preset")
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .suggests(NPC_ID_SUGGESTIONS)
                                                        .then(Commands.argument("preset", ResourceLocationArgument.id())
                                                                .suggests(PRESET_SUGGESTIONS)
                                                                .executes(TalesMakerCommands::setNpcPreset))))
                                        .then(Commands.literal("invulnerable")
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .suggests(NPC_ID_SUGGESTIONS)
                                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                                .executes(TalesMakerCommands::setInvulnerable)))))
)
                        // /talesmaker rotate start <id> <x y z> <once|continuous>
                        // /talesmaker rotate start <id> <entity> <once|continuous>
                        // /talesmaker rotate stop <id>
                        .then(Commands.literal("rotate")
                                .then(Commands.literal("start")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests(NPC_ID_SUGGESTIONS)
                                                // Variant with coordinates
                                                .then(Commands.argument("target", Vec3Argument.vec3())
                                                        .then(Commands.argument("duration", StringArgumentType.word())
                                                                .suggests(LOOK_DURATION_SUGGESTIONS)
                                                                .executes(TalesMakerCommands::rotateToCoords)))
                                                // Variant with entity selector
                                                .then(Commands.argument("entity", EntityArgument.entity())
                                                        .then(Commands.argument("duration", StringArgumentType.word())
                                                                .suggests(LOOK_DURATION_SUGGESTIONS)
                                                                .executes(TalesMakerCommands::rotateToEntity)))))
                                .then(Commands.literal("stop")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests(NPC_ID_SUGGESTIONS)
                                                .executes(TalesMakerCommands::rotateStop))))
                        // /talesmaker movement goto <id> <x y z>
                        // /talesmaker movement goto <id> <entity>
                        // /talesmaker movement stop <id>
                        .then(Commands.literal("movement")
                                .then(Commands.literal("goto")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests(NPC_ID_SUGGESTIONS)
                                                // Variant with coordinates
                                                .then(Commands.argument("target", Vec3Argument.vec3())
                                                        .executes(TalesMakerCommands::movementGotoCoords))
                                                // Variant with entity selector
                                                .then(Commands.argument("entity", EntityArgument.entity())
                                                        .executes(TalesMakerCommands::movementGotoEntity))))
                                .then(Commands.literal("stop")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests(NPC_ID_SUGGESTIONS)
                                                .executes(TalesMakerCommands::movementStop)))
                                // /talesmaker movement patrol <id> <pos1> <pos2> [pos3] ...
                                .then(Commands.literal("patrol")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests(NPC_ID_SUGGESTIONS)
                                                .then(Commands.argument("pos1", Vec3Argument.vec3())
                                                        .then(Commands.argument("pos2", Vec3Argument.vec3())
                                                                .executes(TalesMakerCommands::movementPatrol2)
                                                                .then(Commands.argument("pos3", Vec3Argument.vec3())
                                                                        .executes(TalesMakerCommands::movementPatrol3)
                                                                        .then(Commands.argument("pos4", Vec3Argument.vec3())
                                                                                .executes(TalesMakerCommands::movementPatrol4)
                                                                                .then(Commands.argument("pos5", Vec3Argument.vec3())
                                                                                        .executes(TalesMakerCommands::movementPatrol5))))))))
                                // /talesmaker movement follow <id> <entity>
                                .then(Commands.literal("follow")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests(NPC_ID_SUGGESTIONS)
                                                .then(Commands.argument("target", EntityArgument.entity())
                                                        .executes(TalesMakerCommands::movementFollow))))
                                // /talesmaker movement forward <id> <distance>
                                .then(Commands.literal("forward")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests(NPC_ID_SUGGESTIONS)
                                                .then(Commands.argument("distance", FloatArgumentType.floatArg(0.1f))
                                                        .executes(ctx -> movementDirectional(ctx, "forward")))))
                                // /talesmaker movement backward <id> <distance>
                                .then(Commands.literal("backward")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests(NPC_ID_SUGGESTIONS)
                                                .then(Commands.argument("distance", FloatArgumentType.floatArg(0.1f))
                                                        .executes(ctx -> movementDirectional(ctx, "backward")))))
                                // /talesmaker movement left <id> <distance>
                                .then(Commands.literal("left")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests(NPC_ID_SUGGESTIONS)
                                                .then(Commands.argument("distance", FloatArgumentType.floatArg(0.1f))
                                                        .executes(ctx -> movementDirectional(ctx, "left")))))
                                // /talesmaker movement right <id> <distance>
                                .then(Commands.literal("right")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests(NPC_ID_SUGGESTIONS)
                                                .then(Commands.argument("distance", FloatArgumentType.floatArg(0.1f))
                                                        .executes(ctx -> movementDirectional(ctx, "right"))))))
                        // /talesmaker anim play <id> <animation> [mode]
                        // /talesmaker anim stop <id>
                        .then(Commands.literal("anim")
                                .then(Commands.literal("play")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests(NPC_ID_SUGGESTIONS)
                                                .then(Commands.argument("animation", StringArgumentType.word())
                                                        .executes(TalesMakerCommands::animPlayOnce)
                                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                                .suggests(ANIM_MODE_SUGGESTIONS)
                                                                .executes(TalesMakerCommands::animPlayWithMode)))))
                                .then(Commands.literal("stop")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests(NPC_ID_SUGGESTIONS)
                                                .executes(TalesMakerCommands::animStop))))
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

    private static int createNpcAtPos(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String customId = StringArgumentType.getString(context, "id");
        ResourceLocation presetId = ResourceLocationArgument.getId(context, "preset");
        Vec3 position = Vec3Argument.getVec3(context, "pos");

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

        source.sendSuccess(() -> Component.literal("Created NPC '" + customId + "' at " +
                (int) position.x + ", " + (int) position.y + ", " + (int) position.z +
                " with preset: ").append(preset.name()), true);
        return 1;
    }

    private static int createNpcAtPosWithScript(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String customId = StringArgumentType.getString(context, "id");
        ResourceLocation presetId = ResourceLocationArgument.getId(context, "preset");
        Vec3 position = Vec3Argument.getVec3(context, "pos");
        String scriptType = StringArgumentType.getString(context, "script_type");
        String command = StringArgumentType.getString(context, "command");

        if (!(source.getLevel() instanceof ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("This command can only be used in a server world"));
            return 0;
        }

        // Validate script type
        if (!isValidScriptType(scriptType)) {
            source.sendFailure(Component.literal("Invalid script type. Use 'default', 'interact', or 'player_nearby <radius>'"));
            return 0;
        }

        // Check if NPC with this ID already exists
        boolean idExists = serverLevel.getEntities(ModEntities.NPC.get(), npc -> customId.equals(npc.getCustomId()))
                .stream().findAny().isPresent();
        if (idExists) {
            source.sendFailure(Component.literal("NPC with id '" + customId + "' already exists"));
            return 0;
        }

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

        // Set script
        npc.setScript(scriptType, command);

        // Spawn in world
        serverLevel.addFreshEntity(npc);

        // Execute default script immediately
        if (scriptType.equals("default")) {
            executeNpcScript(source.getServer(), npc, null);
        }

        source.sendSuccess(() -> Component.literal("Created NPC '" + customId + "' at " +
                (int) position.x + ", " + (int) position.y + ", " + (int) position.z +
                " with preset: ").append(preset.name())
                .append(Component.literal(" and " + scriptType + " script")), true);
        return 1;
    }

    private static int setNpcScript(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");
        String scriptType = StringArgumentType.getString(context, "script_type");
        String command = StringArgumentType.getString(context, "command");

        if (!(source.getLevel() instanceof ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("This command can only be used in a server world"));
            return 0;
        }

        // Validate script type
        if (!isValidScriptType(scriptType)) {
            source.sendFailure(Component.literal("Invalid script type. Use 'default', 'interact', or 'player_nearby <radius>'"));
            return 0;
        }

        // Find NPC by custom ID
        NpcEntity npc = serverLevel.getEntities(ModEntities.NPC.get(), entity -> npcId.equals(entity.getCustomId()))
                .stream().findFirst().orElse(null);

        if (npc == null) {
            source.sendFailure(Component.literal("NPC with id '" + npcId + "' not found"));
            return 0;
        }

        // Set script (this also resets InteractUsed flag)
        npc.setScript(scriptType, command);

        // Execute default script immediately
        if (scriptType.equals("default")) {
            executeNpcScript(source.getServer(), npc, null);
        }

        source.sendSuccess(() -> Component.literal("Set " + scriptType + " script for NPC '" + npcId + "'"), true);
        return 1;
    }

    private static int setNpcPreset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");
        ResourceLocation presetId = ResourceLocationArgument.getId(context, "preset");

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

        // Load preset from manager
        NpcPreset preset = TalesMaker.PRESET_MANAGER.getPreset(presetId).orElse(null);

        if (preset == null) {
            source.sendFailure(Component.literal("Unknown preset: " + presetId));
            return 0;
        }

        // Apply preset
        npc.setPreset(preset);

        source.sendSuccess(() -> Component.literal("Set preset for NPC '" + npcId + "' to: ").append(preset.name()), true);
        return 1;
    }

    private static void executeNpcScript(net.minecraft.server.MinecraftServer server, NpcEntity npc, ServerPlayer player) {
        String command = npc.getScriptCommand();
        if (command == null || command.isEmpty()) {
            return;
        }

        // Replace placeholders
        command = command
                .replace("{npc}", npc.getCustomId())
                .replace("{x}", String.valueOf((int) npc.getX()))
                .replace("{y}", String.valueOf((int) npc.getY()))
                .replace("{z}", String.valueOf((int) npc.getZ()));

        if (player != null) {
            command = command.replace("{player}", player.getName().getString());
        }

        // Execute command from server console with error handling
        try {
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack(),
                    command
            );
        } catch (Exception e) {
            TalesMaker.LOGGER.error("Failed to execute NPC script for '{}': {}", npc.getCustomId(), command, e);
        }
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

    // ===== Helper methods =====

    private static boolean isValidScriptType(String scriptType) {
        return scriptType.equals("default")
                || scriptType.equals("interact")
                || scriptType.startsWith("player_nearby");
    }

    // ===== NPC Create with Invulnerable =====

    private static int createNpcWithInvulnerable(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String customId = StringArgumentType.getString(context, "id");
        ResourceLocation presetId = ResourceLocationArgument.getId(context, "preset");
        Vec3 position = Vec3Argument.getVec3(context, "pos");
        boolean invulnerable = BoolArgumentType.getBool(context, "invulnerable");

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

        // Apply preset, custom ID and invulnerable
        npc.setCustomId(customId);
        npc.setPreset(preset);
        npc.setInvulnerable(invulnerable);

        // Spawn in world
        serverLevel.addFreshEntity(npc);

        source.sendSuccess(() -> Component.literal("Created NPC '" + customId + "' at " +
                (int) position.x + ", " + (int) position.y + ", " + (int) position.z +
                " with preset: ").append(preset.name())
                .append(Component.literal(invulnerable ? " (invulnerable)" : "")), true);
        return 1;
    }

    private static int createNpcFullWithInvulnerable(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String customId = StringArgumentType.getString(context, "id");
        ResourceLocation presetId = ResourceLocationArgument.getId(context, "preset");
        Vec3 position = Vec3Argument.getVec3(context, "pos");
        boolean invulnerable = BoolArgumentType.getBool(context, "invulnerable");
        String scriptType = StringArgumentType.getString(context, "script_type");
        String command = StringArgumentType.getString(context, "command");

        if (!(source.getLevel() instanceof ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("This command can only be used in a server world"));
            return 0;
        }

        // Validate script type
        if (!isValidScriptType(scriptType)) {
            source.sendFailure(Component.literal("Invalid script type. Use 'default', 'interact', or 'player_nearby <radius>'"));
            return 0;
        }

        // Check if NPC with this ID already exists
        boolean idExists = serverLevel.getEntities(ModEntities.NPC.get(), npc -> customId.equals(npc.getCustomId()))
                .stream().findAny().isPresent();
        if (idExists) {
            source.sendFailure(Component.literal("NPC with id '" + customId + "' already exists"));
            return 0;
        }

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

        // Apply preset, custom ID, invulnerable and script
        npc.setCustomId(customId);
        npc.setPreset(preset);
        npc.setInvulnerable(invulnerable);
        npc.setScript(scriptType, command);

        // Spawn in world
        serverLevel.addFreshEntity(npc);

        // Execute default script immediately
        if (scriptType.equals("default")) {
            executeNpcScript(source.getServer(), npc, null);
        }

        source.sendSuccess(() -> Component.literal("Created NPC '" + customId + "' at " +
                (int) position.x + ", " + (int) position.y + ", " + (int) position.z +
                " with preset: ").append(preset.name())
                .append(Component.literal((invulnerable ? " (invulnerable)" : "") + " and " + scriptType + " script")), true);
        return 1;
    }

    // ===== NPC Remove =====

    private static int removeNpc(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");

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

        npc.discard();
        source.sendSuccess(() -> Component.literal("Removed NPC '" + npcId + "'"), true);
        return 1;
    }

    // ===== NPC Set Invulnerable =====

    private static int setInvulnerable(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");
        boolean value = BoolArgumentType.getBool(context, "value");

        if (!(source.getLevel() instanceof ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("This command can only be used in a server world"));
            return 0;
        }

        NpcEntity npc = serverLevel.getEntities(ModEntities.NPC.get(), entity -> npcId.equals(entity.getCustomId()))
                .stream().findFirst().orElse(null);

        if (npc == null) {
            source.sendFailure(Component.literal("NPC with id '" + npcId + "' not found"));
            return 0;
        }

        npc.setInvulnerable(value);
        source.sendSuccess(() -> Component.literal("Set invulnerable to " + value + " for NPC '" + npcId + "'"), true);
        return 1;
    }

    // ===== Rotate Commands =====

    private static int rotateToCoords(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");
        Vec3 target = Vec3Argument.getVec3(context, "target");
        String duration = StringArgumentType.getString(context, "duration");

        if (!(source.getLevel() instanceof ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("This command can only be used in a server world"));
            return 0;
        }

        // Validate duration
        if (!duration.equals("once") && !duration.equals("continuous")) {
            source.sendFailure(Component.translatable("commands.talesmaker.rotate.invalid_duration"));
            return 0;
        }

        // Find NPC by custom ID
        NpcEntity npc = serverLevel.getEntities(ModEntities.NPC.get(), entity -> npcId.equals(entity.getCustomId()))
                .stream().findFirst().orElse(null);

        if (npc == null) {
            source.sendFailure(Component.literal("NPC with id '" + npcId + "' not found"));
            return 0;
        }

        boolean continuous = duration.equals("continuous");
        npc.setLookAtTarget(target.x, target.y, target.z, continuous);

        source.sendSuccess(() -> Component.translatable("commands.talesmaker.rotate.success",
                npcId,
                String.format("%.1f, %.1f, %.1f", target.x, target.y, target.z),
                duration), true);
        return 1;
    }

    private static int rotateToEntity(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");
        Entity targetEntity = EntityArgument.getEntity(context, "entity");
        String duration = StringArgumentType.getString(context, "duration");

        if (!(source.getLevel() instanceof ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("This command can only be used in a server world"));
            return 0;
        }

        // Validate duration
        if (!duration.equals("once") && !duration.equals("continuous")) {
            source.sendFailure(Component.translatable("commands.talesmaker.rotate.invalid_duration"));
            return 0;
        }

        // Find NPC by custom ID
        NpcEntity npc = serverLevel.getEntities(ModEntities.NPC.get(), entity -> npcId.equals(entity.getCustomId()))
                .stream().findFirst().orElse(null);

        if (npc == null) {
            source.sendFailure(Component.literal("NPC with id '" + npcId + "' not found"));
            return 0;
        }

        boolean continuous = duration.equals("continuous");
        npc.setLookAtTarget(targetEntity, continuous);

        String targetName = targetEntity.getName().getString();
        source.sendSuccess(() -> Component.translatable("commands.talesmaker.rotate.success_entity",
                npcId,
                targetName,
                duration), true);
        return 1;
    }

    private static int rotateStop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");

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

        npc.stopLookAt();
        source.sendSuccess(() -> Component.translatable("commands.talesmaker.rotate.stop.success", npcId), true);
        return 1;
    }

    private static int movementGotoCoords(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");
        Vec3 target = Vec3Argument.getVec3(context, "target");

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

        npc.moveToPosition(target.x, target.y, target.z);

        source.sendSuccess(() -> Component.translatable("commands.talesmaker.movement.goto.success",
                npcId,
                String.format("%.1f, %.1f, %.1f", target.x, target.y, target.z)), true);
        return 1;
    }

    private static int movementGotoEntity(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");
        Entity targetEntity = EntityArgument.getEntity(context, "entity");

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

        npc.moveToPosition(targetEntity.getX(), targetEntity.getY(), targetEntity.getZ());

        String targetName = targetEntity.getName().getString();
        source.sendSuccess(() -> Component.translatable("commands.talesmaker.movement.goto.success_entity",
                npcId,
                targetName), true);
        return 1;
    }

    private static int movementStop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");

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

        npc.stopMovement();
        source.sendSuccess(() -> Component.translatable("commands.talesmaker.movement.stop.success", npcId), true);
        return 1;
    }

    private static int movementPatrol2(CommandContext<CommandSourceStack> context) {
        return executePatrol(context, 2);
    }

    private static int movementPatrol3(CommandContext<CommandSourceStack> context) {
        return executePatrol(context, 3);
    }

    private static int movementPatrol4(CommandContext<CommandSourceStack> context) {
        return executePatrol(context, 4);
    }

    private static int movementPatrol5(CommandContext<CommandSourceStack> context) {
        return executePatrol(context, 5);
    }

    private static int executePatrol(CommandContext<CommandSourceStack> context, int pointCount) {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");

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

        // Collect patrol points as Vec3 for sub-block precision
        java.util.List<Vec3> points = new java.util.ArrayList<>();
        for (int i = 1; i <= pointCount; i++) {
            Vec3 pos = Vec3Argument.getVec3(context, "pos" + i);
            points.add(pos);
        }

        npc.startPatrolVec3(points);
        source.sendSuccess(() -> Component.translatable("commands.talesmaker.movement.patrol.success",
                npcId,
                pointCount), true);
        return 1;
    }

    private static int movementFollow(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");
        Entity targetEntity = EntityArgument.getEntity(context, "target");

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

        npc.startFollow(targetEntity);
        String targetName = targetEntity.getName().getString();
        source.sendSuccess(() -> Component.translatable("commands.talesmaker.movement.follow.success",
                npcId,
                targetName), true);
        return 1;
    }

    private static int movementDirectional(CommandContext<CommandSourceStack> context, String direction) {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");
        float distance = FloatArgumentType.getFloat(context, "distance");

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

        npc.startDirectionalMovement(direction, distance);
        source.sendSuccess(() -> Component.translatable("commands.talesmaker.movement.directional.success",
                npcId,
                direction,
                String.format("%.1f", distance)), true);
        return 1;
    }

    // ===== Animation Commands =====

    private static int animPlayOnce(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");
        String animation = StringArgumentType.getString(context, "animation");

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

        // Use new animation system
        npc.getAnimationManager().playCustomAnimation(animation, "once");
        source.sendSuccess(() -> Component.translatable("commands.talesmaker.anim.play.success",
                npcId,
                animation,
                "once"), true);
        return 1;
    }

    private static int animPlayWithMode(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");
        String animation = StringArgumentType.getString(context, "animation");
        String mode = StringArgumentType.getString(context, "mode");

        if (!(source.getLevel() instanceof ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("This command can only be used in a server world"));
            return 0;
        }

        // Validate mode
        if (!mode.equals("once") && !mode.equals("loop") && !mode.equals("hold")) {
            source.sendFailure(Component.translatable("commands.talesmaker.anim.invalid_mode"));
            return 0;
        }

        // Find NPC by custom ID
        NpcEntity npc = serverLevel.getEntities(ModEntities.NPC.get(), entity -> npcId.equals(entity.getCustomId()))
                .stream().findFirst().orElse(null);

        if (npc == null) {
            source.sendFailure(Component.literal("NPC with id '" + npcId + "' not found"));
            return 0;
        }

        // Use new animation system
        npc.getAnimationManager().playCustomAnimation(animation, mode);
        source.sendSuccess(() -> Component.translatable("commands.talesmaker.anim.play.success",
                npcId,
                animation,
                mode), true);
        return 1;
    }

    private static int animStop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String npcId = StringArgumentType.getString(context, "id");

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

        // Use new animation system
        npc.getAnimationManager().stopCustomAnimation();
        source.sendSuccess(() -> Component.translatable("commands.talesmaker.anim.stop.success", npcId), true);
        return 1;
    }
}
