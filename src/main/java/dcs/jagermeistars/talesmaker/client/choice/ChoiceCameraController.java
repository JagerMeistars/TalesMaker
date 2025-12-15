package dcs.jagermeistars.talesmaker.client.choice;

import dcs.jagermeistars.talesmaker.entity.NpcEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Controls the cinematic camera during choice windows.
 *
 * When active:
 * - Player switches to spectator mode and teleports to position in front of NPC
 * - Player looks opposite to NPC's look direction (facing the NPC)
 * - NPC turns to look at player using NpcLookHandler
 * - Camera returns to original position when choice window closes
 */
public class ChoiceCameraController {

    // Animation constants
    private static final float ANIMATION_DURATION_MS = 250.0f; // 0.25 seconds

    private static boolean active = false;
    private static Entity speaker;
    private static Vec3 targetPlayerPos;
    private static float targetYaw;
    private static float targetPitch;

    // Animation state
    private static boolean animatingIn = false;
    private static boolean animatingOut = false;
    private static long animationStartTime = 0;
    private static Vec3 animStartPos;
    private static float animStartYaw;
    private static float animStartPitch;

    // Original state to restore
    private static CameraType originalCameraType;
    private static Vec3 originalPlayerPos;
    private static float originalYaw;
    private static float originalPitch;
    private static GameType originalGameMode;

    // NPC original rotation
    private static float originalSpeakerYaw;
    private static float originalSpeakerPitch;
    private static float originalSpeakerHeadYaw;

    // NPC rotation at start of fly-out animation (when looking at player)
    private static float animStartSpeakerYaw;
    private static float animStartSpeakerPitch;
    private static float animStartSpeakerHeadYaw;

    /**
     * Start the cinematic camera focusing on the speaker entity.
     */
    public static void startCinematic(Entity speakerEntity) {
        if (speakerEntity == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;

        active = true;
        speaker = speakerEntity;

        // Save original player state
        originalCameraType = mc.options.getCameraType();
        originalPlayerPos = mc.player.position();
        originalYaw = mc.player.getYRot();
        originalPitch = mc.player.getXRot();
        originalGameMode = mc.gameMode.getPlayerMode();

        // Save original NPC rotation
        originalSpeakerYaw = speaker.getYRot();
        originalSpeakerPitch = speaker.getXRot();
        originalSpeakerHeadYaw = speaker.getYHeadRot();

        // Calculate target position and rotation
        calculateCinematicPosition(mc);

        // Save animation start position (current player position)
        animStartPos = mc.player.position();
        animStartYaw = mc.player.getYRot();
        animStartPitch = mc.player.getXRot();

        // Start fly-in animation
        animatingIn = true;
        animatingOut = false;
        animationStartTime = System.currentTimeMillis();

        // Switch to spectator mode for free camera movement
        mc.gameMode.setLocalMode(GameType.SPECTATOR);

        // Stay in first person
        mc.options.setCameraType(CameraType.FIRST_PERSON);

        // Make NPC look at player using NpcEntity's look-at system
        if (speaker instanceof NpcEntity npc) {
            npc.setLookAtTarget(mc.player, true);
        }
    }

    /**
     * Calculate the cinematic position: in front of NPC, offset to the right.
     * Player looks opposite to NPC's view direction.
     */
    private static void calculateCinematicPosition(Minecraft mc) {
        if (speaker == null || mc.player == null) return;

        // Get NPC's look direction (yaw in degrees)
        float npcYaw = speaker.getYRot();

        // Convert to radians for calculations
        double yawRad = Math.toRadians(npcYaw);

        // Forward vector (direction NPC is looking)
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);

        // Right vector (perpendicular to forward, to NPC's right)
        double rightX = -forwardZ;
        double rightZ = forwardX;

        // Target position: 1.5 blocks in front of NPC + 1 block to the right
        Vec3 npcPos = speaker.position();
        double targetX = npcPos.x + forwardX * 1.5 + rightX * 1.0;
        double targetY = npcPos.y; // Same Y level as NPC feet (spectator camera has no eye offset)
        double targetZ = npcPos.z + forwardZ * 1.5 + rightZ * 1.0;

        targetPlayerPos = new Vec3(targetX, targetY, targetZ);

        // Player looks opposite to NPC's direction (facing the NPC)
        targetYaw = npcYaw + 180.0f;
        // Normalize yaw to -180 to 180
        while (targetYaw > 180.0f) targetYaw -= 360.0f;
        while (targetYaw < -180.0f) targetYaw += 360.0f;

        // Pitch at eye level (horizontal)
        targetPitch = 0.0f;
    }

    /**
     * Stop the cinematic camera and start fly-out animation back to original position.
     */
    public static void stopCinematic() {
        if (!active) return;

        // If already animating out, don't restart
        if (animatingOut) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            // No player - just cleanup immediately
            finishStopCinematic();
            return;
        }

        // Stop NPC look-at behavior
        if (speaker != null && speaker instanceof NpcEntity npc) {
            npc.setLookAtActive(false);
        }

        // Save NPC's current rotation (looking at player) for animation
        if (speaker != null) {
            animStartSpeakerYaw = speaker.getYRot();
            animStartSpeakerPitch = speaker.getXRot();
            animStartSpeakerHeadYaw = speaker.getYHeadRot();
        }

        // Save current position as animation start (cinematic position)
        animStartPos = mc.player.position();
        animStartYaw = mc.player.getYRot();
        animStartPitch = mc.player.getXRot();

        // Start fly-out animation
        animatingIn = false;
        animatingOut = true;
        animationStartTime = System.currentTimeMillis();
    }

    /**
     * Complete the stop cinematic process after fly-out animation finishes.
     */
    private static void finishStopCinematic() {
        Minecraft mc = Minecraft.getInstance();

        // Restore player game mode
        if (mc.gameMode != null && originalGameMode != null) {
            mc.gameMode.setLocalMode(originalGameMode);
        }

        // Restore player position and rotation
        if (mc.player != null && originalPlayerPos != null) {
            mc.player.setPos(originalPlayerPos);
            mc.player.setYRot(originalYaw);
            mc.player.setXRot(originalPitch);
            mc.player.yRotO = originalYaw;
            mc.player.xRotO = originalPitch;
        }

        // Restore NPC rotation
        if (speaker != null) {
            speaker.setYRot(originalSpeakerYaw);
            speaker.setXRot(originalSpeakerPitch);
            speaker.yRotO = originalSpeakerYaw;
            speaker.xRotO = originalSpeakerPitch;
            speaker.setYHeadRot(originalSpeakerHeadYaw);
        }

        // Reset state
        active = false;
        animatingIn = false;
        animatingOut = false;
        speaker = null;

        // Restore original camera type
        if (originalCameraType != null) {
            mc.options.setCameraType(originalCameraType);
        }
    }

    /**
     * Update camera animation every render frame for smooth movement.
     * Should be called from RenderFrameEvent.Pre.
     */
    public static void renderTick(float partialTick) {
        if (!active) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float elapsed = System.currentTimeMillis() - animationStartTime;
        float progress = Math.min(elapsed / ANIMATION_DURATION_MS, 1.0f);

        if (animatingIn) {
            // Fly-in animation: from player's original position to cinematic position
            double x = Mth.lerp(progress, animStartPos.x, targetPlayerPos.x);
            double y = Mth.lerp(progress, animStartPos.y, targetPlayerPos.y);
            double z = Mth.lerp(progress, animStartPos.z, targetPlayerPos.z);

            float yaw = Mth.rotLerp(progress, animStartYaw, targetYaw);
            float pitch = Mth.lerp(progress, animStartPitch, targetPitch);

            mc.player.setPos(x, y, z);
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
            mc.player.yRotO = yaw;
            mc.player.xRotO = pitch;

        } else if (animatingOut) {
            // Fly-out animation: from cinematic position back to original position
            double x = Mth.lerp(progress, animStartPos.x, originalPlayerPos.x);
            double y = Mth.lerp(progress, animStartPos.y, originalPlayerPos.y);
            double z = Mth.lerp(progress, animStartPos.z, originalPlayerPos.z);

            float yaw = Mth.rotLerp(progress, animStartYaw, originalYaw);
            float pitch = Mth.lerp(progress, animStartPitch, originalPitch);

            mc.player.setPos(x, y, z);
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
            mc.player.yRotO = yaw;
            mc.player.xRotO = pitch;

            // Smoothly animate NPC rotation back to original
            if (speaker != null) {
                float npcYaw = Mth.rotLerp(progress, animStartSpeakerYaw, originalSpeakerYaw);
                float npcPitch = Mth.lerp(progress, animStartSpeakerPitch, originalSpeakerPitch);
                float npcHeadYaw = Mth.rotLerp(progress, animStartSpeakerHeadYaw, originalSpeakerHeadYaw);

                speaker.setYRot(npcYaw);
                speaker.setXRot(npcPitch);
                speaker.yRotO = npcYaw;
                speaker.xRotO = npcPitch;
                speaker.setYHeadRot(npcHeadYaw);
            }

        } else {
            // Not animating - keep player locked at target position
            if (speaker == null) return;

            mc.player.setPos(targetPlayerPos);
            mc.player.setYRot(targetYaw);
            mc.player.setXRot(targetPitch);
            mc.player.yRotO = targetYaw;
            mc.player.xRotO = targetPitch;
        }
    }

    /**
     * Update animation state each tick.
     * Should be called from client tick event.
     */
    public static void tick() {
        if (!active) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (animatingIn) {
            // Check if fly-in animation is complete
            float elapsed = System.currentTimeMillis() - animationStartTime;
            float progress = Math.min(elapsed / ANIMATION_DURATION_MS, 1.0f);

            if (progress >= 1.0f) {
                animatingIn = false;
            }

        } else if (animatingOut) {
            // Check if fly-out animation is complete
            float elapsed = System.currentTimeMillis() - animationStartTime;
            float progress = Math.min(elapsed / ANIMATION_DURATION_MS, 1.0f);

            if (progress >= 1.0f) {
                // Animation complete - finish cleanup
                finishStopCinematic();
                return;
            }

        } else {
            // Not animating - handle NPC look-at for non-NpcEntity speakers
            if (speaker != null && !(speaker instanceof NpcEntity)) {
                makeNpcLookAtPlayer(mc);
            }
        }
    }

    /**
     * Make the NPC turn to face the player (for non-NpcEntity speakers).
     */
    private static void makeNpcLookAtPlayer(Minecraft mc) {
        if (speaker == null || mc.player == null) return;

        Vec3 playerPos = mc.player.getEyePosition(1.0f);
        Vec3 npcPos = speaker.position().add(0, speaker.getEyeHeight(), 0);

        // Calculate direction from NPC to player
        Vec3 lookDir = playerPos.subtract(npcPos);
        double horizontalDist = Math.sqrt(lookDir.x * lookDir.x + lookDir.z * lookDir.z);

        // Calculate yaw and pitch for NPC to look at player
        float npcTargetYaw = (float) (Math.atan2(-lookDir.x, lookDir.z) * (180.0 / Math.PI));
        float npcTargetPitch = (float) -(Math.atan2(lookDir.y, horizontalDist) * (180.0 / Math.PI));

        // Apply rotation to NPC
        speaker.setYRot(npcTargetYaw);
        speaker.setXRot(npcTargetPitch);
        speaker.yRotO = npcTargetYaw;
        speaker.xRotO = npcTargetPitch;

        // Also set head rotation for proper look
        speaker.setYHeadRot(npcTargetYaw);
    }

    /**
     * Get the current camera position for rendering.
     */
    public static Vec3 getCameraPosition(float partialTick) {
        if (!active || targetPlayerPos == null) return null;
        return targetPlayerPos;
    }

    /**
     * Get the current camera yaw rotation.
     */
    public static float getCameraYaw(float partialTick) {
        return targetYaw;
    }

    /**
     * Get the current camera pitch rotation.
     */
    public static float getCameraPitch(float partialTick) {
        return targetPitch;
    }

    /**
     * Check if the cinematic camera is active.
     */
    public static boolean isActive() {
        return active;
    }

    /**
     * Check if player movement should be frozen.
     */
    public static boolean shouldFreezePlayer() {
        return active;
    }
}
