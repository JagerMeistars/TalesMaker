package dcs.jagermeistars.talesmaker.client.bind;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

public final class BindCommands {
    private static final SuggestionProvider<CommandSourceStack> BIND_KEYS = (context, builder) ->
            SharedSuggestionProvider.suggest(BindManager.getBindKeyNames(), builder);

    private static final SuggestionProvider<CommandSourceStack> AVAILABLE_KEYS = (context, builder) ->
            SharedSuggestionProvider.suggest(BindManager.getAvailableKeyNames(), builder);

    private BindCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("talesmaker")
                        .then(Commands.literal("bind")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("key", StringArgumentType.word())
                                                .suggests(AVAILABLE_KEYS)
                                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                                        .executes(ctx -> {
                                                            String key = StringArgumentType.getString(ctx, "key");
                                                            String command = StringArgumentType.getString(ctx, "command");
                                                            BindManager.BindAddResult result = BindManager.addBind(key, command);
                                                            if (result == BindManager.BindAddResult.OK) {
                                                                return 1;
                                                            }
                                                            if (result == BindManager.BindAddResult.USED) {
                                                                BindManager.sendMessage("Key already in use: " + key, true);
                                                                return 0;
                                                            }
                                                            BindManager.sendMessage("Unknown key: " + key, true);
                                                            return 0;
                                                        }))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("key", StringArgumentType.word())
                                                .suggests(BIND_KEYS)
                                                .executes(ctx -> {
                                                    String key = StringArgumentType.getString(ctx, "key");
                                                    boolean ok = BindManager.removeBind(key);
                                                    if (ok) {
                                                        return 1;
                                                    }
                                                    BindManager.sendMessage("No bind found for " + key, true);
                                                    return 0;
                                                })))
                        )
        );
    }
}
