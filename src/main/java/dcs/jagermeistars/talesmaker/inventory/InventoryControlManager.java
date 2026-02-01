package dcs.jagermeistars.talesmaker.inventory;

import dcs.jagermeistars.talesmaker.network.InventoryControlPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public final class InventoryControlManager {

    private InventoryControlManager() {}

    public static InventoryRestrictions get(ServerPlayer player) {
        InventoryControlSavedData data = getData(player);
        return data.get(player.getUUID());
    }

    public static void applyToPlayer(ServerPlayer player, InventoryRestrictions restrictions) {
        InventoryRestrictions normalized = restrictions.normalized();
        InventoryControlSavedData data = getData(player);
        data.set(player.getUUID(), normalized);
        PacketDistributor.sendToPlayer(player, InventoryControlPacket.fromRestrictions(normalized));
        InventoryControlEnforcer.enforceNow(player, normalized);
    }

    public static void sendToPlayer(ServerPlayer player) {
        InventoryRestrictions restrictions = get(player).normalized();
        PacketDistributor.sendToPlayer(player, InventoryControlPacket.fromRestrictions(restrictions));
    }

    public static void clear(ServerPlayer player) {
        InventoryControlSavedData data = getData(player);
        data.clear(player.getUUID());
    }

    private static InventoryControlSavedData getData(ServerPlayer player) {
        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return InventoryControlSavedData.get(player.serverLevel());
        }
        return InventoryControlSavedData.get(overworld);
    }
}
