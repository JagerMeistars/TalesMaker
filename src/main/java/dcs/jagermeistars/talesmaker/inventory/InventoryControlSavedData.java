package dcs.jagermeistars.talesmaker.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryControlSavedData extends SavedData {

    private static final String NAME = "talesmaker_inventory_control";
    private static final String KEY_PLAYERS = "players";

    private final Map<UUID, InventoryRestrictions> settings = new HashMap<>();

    public static InventoryControlSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(InventoryControlSavedData::new, InventoryControlSavedData::load),
                NAME);
    }

    public InventoryRestrictions get(UUID playerId) {
        return settings.getOrDefault(playerId, InventoryRestrictions.DEFAULT);
    }

    public void set(UUID playerId, InventoryRestrictions restrictions) {
        InventoryRestrictions normalized = restrictions.normalized();
        if (normalized.isDefault()) {
            settings.remove(playerId);
        } else {
            settings.put(playerId, normalized);
        }
        setDirty();
    }

    public void clear(UUID playerId) {
        if (settings.remove(playerId) != null) {
            setDirty();
        }
    }

    public void clearAll() {
        if (!settings.isEmpty()) {
            settings.clear();
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag playersTag = new CompoundTag();
        for (Map.Entry<UUID, InventoryRestrictions> entry : settings.entrySet()) {
            InventoryRestrictions r = entry.getValue().normalized();
            CompoundTag data = new CompoundTag();
            data.putBoolean("blockInventoryOpen", r.blockInventoryOpen());
            data.putBoolean("hideHotbar", r.hideHotbar());
            data.putInt("hotbarSlots", r.hotbarSlots());
            data.putInt("inventorySlots", r.inventorySlots());
            playersTag.put(entry.getKey().toString(), data);
        }
        tag.put(KEY_PLAYERS, playersTag);
        return tag;
    }

    public static InventoryControlSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        InventoryControlSavedData data = new InventoryControlSavedData();
        CompoundTag playersTag = tag.getCompound(KEY_PLAYERS);
        for (String key : playersTag.getAllKeys()) {
            try {
                UUID playerId = UUID.fromString(key);
                CompoundTag entry = playersTag.getCompound(key);
                InventoryRestrictions r = new InventoryRestrictions(
                        entry.getBoolean("blockInventoryOpen"),
                        entry.getInt("hotbarSlots"),
                        entry.getInt("inventorySlots"),
                        entry.getBoolean("hideHotbar")
                ).normalized();
                data.settings.put(playerId, r);
            } catch (IllegalArgumentException ignored) {
                // Skip invalid UUID keys
            }
        }
        return data;
    }
}
