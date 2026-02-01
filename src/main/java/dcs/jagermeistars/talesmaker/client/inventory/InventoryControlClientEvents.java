package dcs.jagermeistars.talesmaker.client.inventory;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.inventory.InventoryRestrictions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = TalesMaker.MODID, value = Dist.CLIENT)
public class InventoryControlClientEvents {

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        InventoryRestrictions restrictions = ClientInventoryControl.getRestrictions();
        if (!restrictions.blockInventoryOpen()) {
            return;
        }

        if (event.getScreen() instanceof InventoryScreen
                || event.getScreen() instanceof CreativeModeInventoryScreen) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        InventoryRestrictions restrictions = ClientInventoryControl.getRestrictions();
        if (!restrictions.hideHotbar()) {
            return;
        }
        if (event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        InventoryRestrictions restrictions = ClientInventoryControl.getRestrictions();
        if (restrictions.isDefault()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        int hotbarLimit = restrictions.hotbarSlots();
        if (hotbarLimit <= 0) {
            mc.player.getInventory().selected = 0;
        } else if (mc.player.getInventory().selected >= hotbarLimit) {
            mc.player.getInventory().selected = Math.max(0, hotbarLimit - 1);
        }
    }
}
