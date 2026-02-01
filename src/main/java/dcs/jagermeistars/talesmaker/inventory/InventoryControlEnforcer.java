package dcs.jagermeistars.talesmaker.inventory;

import dcs.jagermeistars.talesmaker.TalesMaker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.server.level.ServerPlayer;

@EventBusSubscriber(modid = TalesMaker.MODID)
public class InventoryControlEnforcer {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        InventoryRestrictions restrictions = InventoryControlManager.get(player);
        if (restrictions.isDefault()) {
            return;
        }
        enforceNow(player, restrictions);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            InventoryControlManager.sendToPlayer(player);
        }
    }

    public static void enforceNow(ServerPlayer player, InventoryRestrictions restrictions) {
        InventoryRestrictions normalized = restrictions.normalized();
        Inventory inventory = player.getInventory();

        int hotbarLimit = normalized.hotbarSlots();
        int inventoryLimit = normalized.inventorySlots();

        if (hotbarLimit <= 0) {
            inventory.selected = 0;
        } else if (inventory.selected >= hotbarLimit) {
            inventory.selected = Math.max(0, hotbarLimit - 1);
        }

        for (int slot = 0; slot < 9; slot++) {
            if (slot >= hotbarLimit) {
                ejectDisallowedSlot(player, inventory, slot, hotbarLimit, inventoryLimit);
            }
        }

        for (int slot = 9; slot < 36; slot++) {
            if (slot >= 9 + inventoryLimit) {
                ejectDisallowedSlot(player, inventory, slot, hotbarLimit, inventoryLimit);
            }
        }
    }

    private static void ejectDisallowedSlot(ServerPlayer player, Inventory inventory, int slotIndex,
                                            int hotbarLimit, int inventoryLimit) {
        ItemStack stack = inventory.getItem(slotIndex);
        if (stack.isEmpty()) {
            return;
        }
        ItemStack remaining = moveToAllowedSlots(inventory, stack, hotbarLimit, inventoryLimit);
        inventory.setItem(slotIndex, ItemStack.EMPTY);
        if (!remaining.isEmpty()) {
            player.drop(remaining, true);
        }
    }

    private static ItemStack moveToAllowedSlots(Inventory inventory, ItemStack stack,
                                                int hotbarLimit, int inventoryLimit) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack remaining = stack.copy();
        remaining.setCount(stack.getCount());

        remaining = tryMergeIntoRange(inventory, remaining, 0, hotbarLimit);
        remaining = tryMergeIntoRange(inventory, remaining, 9, 9 + inventoryLimit);
        return remaining;
    }

    private static ItemStack tryMergeIntoRange(Inventory inventory, ItemStack stack, int start, int endExclusive) {
        if (stack.isEmpty() || start >= endExclusive) {
            return stack;
        }

        for (int slot = start; slot < endExclusive && !stack.isEmpty(); slot++) {
            ItemStack target = inventory.getItem(slot);
            if (target.isEmpty()) {
                inventory.setItem(slot, stack.copy());
                return ItemStack.EMPTY;
            }
        }

        for (int slot = start; slot < endExclusive && !stack.isEmpty(); slot++) {
            ItemStack target = inventory.getItem(slot);
            if (!ItemStack.isSameItemSameComponents(target, stack)) {
                continue;
            }

            int max = Math.min(target.getMaxStackSize(), inventory.getMaxStackSize());
            if (target.getCount() >= max) {
                continue;
            }

            int space = max - target.getCount();
            int move = Math.min(space, stack.getCount());
            target.grow(move);
            stack.shrink(move);
            inventory.setItem(slot, target);
        }

        return stack;
    }
}
