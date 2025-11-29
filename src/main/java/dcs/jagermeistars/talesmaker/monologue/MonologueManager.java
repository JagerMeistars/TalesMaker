package dcs.jagermeistars.talesmaker.monologue;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.data.NpcPreset;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MonologueManager {

    private static final Map<UUID, ResourceLocation> activeMonologues = new ConcurrentHashMap<>();

    public static void enableMonologue(UUID playerId, ResourceLocation presetId) {
        activeMonologues.put(playerId, presetId);
    }

    public static void disableMonologue(UUID playerId) {
        activeMonologues.remove(playerId);
    }

    public static boolean hasMonologue(UUID playerId) {
        return activeMonologues.containsKey(playerId);
    }

    public static ResourceLocation getPresetId(UUID playerId) {
        return activeMonologues.get(playerId);
    }

    public static NpcPreset getPreset(UUID playerId) {
        ResourceLocation presetId = activeMonologues.get(playerId);
        if (presetId == null) {
            return null;
        }
        return TalesMaker.PRESET_MANAGER.getPreset(presetId).orElse(null);
    }

    public static void clear() {
        activeMonologues.clear();
    }
}
