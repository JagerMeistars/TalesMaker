package dcs.jagermeistars.talesmaker.client.inventory;

import dcs.jagermeistars.talesmaker.inventory.InventoryRestrictions;
import dcs.jagermeistars.talesmaker.network.InventoryControlPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public final class ClientInventoryControl {

    private static InventoryRestrictions restrictions = InventoryRestrictions.DEFAULT;

    private ClientInventoryControl() {}

    public static void applyPacket(InventoryControlPacket packet) {
        restrictions = new InventoryRestrictions(
                packet.blockInventoryOpen(),
                packet.hotbarSlots(),
                packet.inventorySlots(),
                packet.hideHotbar()
        ).normalized();

        Minecraft mc = Minecraft.getInstance();
        if (restrictions.blockInventoryOpen() && mc.screen != null) {
            if (mc.screen instanceof InventoryScreen || mc.screen instanceof CreativeModeInventoryScreen) {
                mc.setScreen(null);
            }
        }
    }

    public static InventoryRestrictions getRestrictions() {
        return restrictions;
    }
}
