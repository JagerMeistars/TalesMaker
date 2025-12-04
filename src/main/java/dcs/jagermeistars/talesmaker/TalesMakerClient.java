package dcs.jagermeistars.talesmaker;

import dcs.jagermeistars.talesmaker.client.dialogue.DialogueHistory;
import dcs.jagermeistars.talesmaker.client.dialogue.DialogueHistoryScreen;
import dcs.jagermeistars.talesmaker.client.model.NpcModel;
import dcs.jagermeistars.talesmaker.client.notification.ResourceErrorManager;
import dcs.jagermeistars.talesmaker.client.renderer.NpcRenderer;
import dcs.jagermeistars.talesmaker.entity.NpcEntity;
import dcs.jagermeistars.talesmaker.init.ModEntities;
import dcs.jagermeistars.talesmaker.network.InteractScriptPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Client-side initialization for TalesMaker mod.
 */
@Mod(value = TalesMaker.MODID, dist = Dist.CLIENT)
public class TalesMakerClient {

    public static final KeyMapping HISTORY_KEY = new KeyMapping(
            "key.talesmaker.history",
            GLFW.GLFW_KEY_H,
            "key.categories.talesmaker"
    );

    public static final KeyMapping INTERACT_KEY = new KeyMapping(
            "key.talesmaker.interact",
            GLFW.GLFW_KEY_X,
            "key.categories.talesmaker"
    );

    public TalesMakerClient() {
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    @EventBusSubscriber(modid = TalesMaker.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(ModEntities.NPC.get(), NpcRenderer::new);
        }

        @SubscribeEvent
        static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(HISTORY_KEY);
            event.register(INTERACT_KEY);
        }
    }

    private void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // Clear cache when joining a world
        NpcModel.clearValidationCache();
        // Clear resource error cache to allow new errors to be displayed
        ResourceErrorManager.clearCache();

        // Load dialogue history for this world
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) {
            DialogueHistory.onWorldJoin(mc.getCurrentServer().ip.replace(":", "_"));
        } else if (mc.getSingleplayerServer() != null) {
            DialogueHistory.onWorldJoin(mc.getSingleplayerServer().getWorldData().getLevelName());
        }
    }

    private void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DialogueHistory.onWorldLeave();
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (HISTORY_KEY.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new DialogueHistoryScreen());
            }
        }

        if (INTERACT_KEY.consumeClick()) {
            if (mc.player != null && mc.level != null && mc.screen == null) {
                NpcEntity targetNpc = findLookedAtNpc(mc);
                if (targetNpc != null && targetNpc.hasInteractScript() && !targetNpc.isInteractUsed()) {
                    PacketDistributor.sendToServer(new InteractScriptPacket(targetNpc.getId()));
                }
            }
        }
    }

    private NpcEntity findLookedAtNpc(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return null;
        }

        double reachDistance = 6.0;
        Vec3 eyePos = mc.player.getEyePosition(1.0f);
        Vec3 lookVec = mc.player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.scale(reachDistance));

        AABB searchBox = mc.player.getBoundingBox().expandTowards(lookVec.scale(reachDistance)).inflate(1.0);

        Predicate<Entity> filter = entity -> entity instanceof NpcEntity && !entity.isSpectator() && entity.isPickable();

        double closestDistance = reachDistance;
        NpcEntity closestNpc = null;

        for (Entity entity : mc.level.getEntities(mc.player, searchBox, filter)) {
            AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hitResult = entityBox.clip(eyePos, endPos);

            if (hitResult.isPresent()) {
                double distance = eyePos.distanceTo(hitResult.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestNpc = (NpcEntity) entity;
                }
            }
        }

        return closestNpc;
    }
}
