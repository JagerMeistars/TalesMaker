package dcs.jagermeistars.talesmaker.inventory;

import net.minecraft.util.Mth;

public record InventoryRestrictions(
        boolean blockInventoryOpen,
        int hotbarSlots,
        int inventorySlots,
        boolean hideHotbar
) {

    public static final InventoryRestrictions DEFAULT = new InventoryRestrictions(false, 9, 27, false);

    public InventoryRestrictions normalized() {
        int clampedHotbar = Mth.clamp(hotbarSlots, 0, 9);
        int clampedInventory = Mth.clamp(inventorySlots, 0, 27);
        return new InventoryRestrictions(blockInventoryOpen, clampedHotbar, clampedInventory, hideHotbar);
    }

    public boolean isDefault() {
        InventoryRestrictions normalized = normalized();
        return normalized.blockInventoryOpen == DEFAULT.blockInventoryOpen
                && normalized.hotbarSlots == DEFAULT.hotbarSlots
                && normalized.inventorySlots == DEFAULT.inventorySlots
                && normalized.hideHotbar == DEFAULT.hideHotbar;
    }

    public InventoryRestrictions withBlockInventoryOpen(boolean value) {
        return new InventoryRestrictions(value, hotbarSlots, inventorySlots, hideHotbar);
    }

    public InventoryRestrictions withHotbarSlots(int value) {
        return new InventoryRestrictions(blockInventoryOpen, value, inventorySlots, hideHotbar);
    }

    public InventoryRestrictions withInventorySlots(int value) {
        return new InventoryRestrictions(blockInventoryOpen, hotbarSlots, value, hideHotbar);
    }

    public InventoryRestrictions withHideHotbar(boolean value) {
        return new InventoryRestrictions(blockInventoryOpen, hotbarSlots, inventorySlots, value);
    }
}
