package dcs.jagermeistars.talesmaker.tutorial;

import dcs.jagermeistars.talesmaker.data.tutorial.TutorialPreset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TutorialSessionManager {

    private static final Map<UUID, ResourceLocation> forcedSessions = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> allowInteraction = new ConcurrentHashMap<>();

    private TutorialSessionManager() {}

    public static void start(ServerPlayer player, TutorialPreset preset) {
        if (preset.isForced()) {
            forcedSessions.put(player.getUUID(), preset.id());
        } else {
            forcedSessions.remove(player.getUUID());
        }
        allowInteraction.put(player.getUUID(), false);
    }

    public static boolean isForcedActive(ServerPlayer player) {
        return forcedSessions.containsKey(player.getUUID());
    }

    public static boolean isInteractionAllowed(ServerPlayer player) {
        return allowInteraction.getOrDefault(player.getUUID(), false);
    }

    public static void setAllowInteraction(ServerPlayer player, boolean allow) {
        allowInteraction.put(player.getUUID(), allow);
    }

    public static void complete(ServerPlayer player) {
        forcedSessions.remove(player.getUUID());
        allowInteraction.remove(player.getUUID());
    }

    public static void onDisconnect(ServerPlayer player) {
        forcedSessions.remove(player.getUUID());
        allowInteraction.remove(player.getUUID());
    }
}
