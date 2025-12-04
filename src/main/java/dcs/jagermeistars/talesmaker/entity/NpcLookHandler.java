package dcs.jagermeistars.talesmaker.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Handles look-at behavior for NPC entities.
 * Includes targeting positions, entities, and view range limits.
 */
public class NpcLookHandler {

    private final NpcEntity npc;

    public NpcLookHandler(NpcEntity npc) {
        this.npc = npc;
    }

    /**
     * Sets up the look-at target to specific coordinates.
     */
    public void lookAt(double x, double y, double z, boolean continuous) {
        npc.setLookAtTargetPosition((float) x, (float) y, (float) z);
        npc.setLookAtEntityId(-1);
        npc.setLookAtContinuous(continuous);
        npc.setLookAtActive(true);
    }

    /**
     * Sets up the look-at target to follow an entity.
     */
    public void lookAt(Entity target, boolean continuous) {
        if (target == null) {
            return;
        }
        npc.setLookAtTargetPosition((float) target.getX(), (float) target.getEyeY(), (float) target.getZ());
        npc.setLookAtEntityId(target.getId());
        npc.setLookAtContinuous(continuous);
        npc.setLookAtActive(true);
    }

    /**
     * Stops the look-at behavior.
     */
    public void stop() {
        npc.setLookAtActive(false);
        npc.setLookAtEntityId(-1);
    }

    /**
     * Updates the look-at behavior. Called every tick when look-at is active.
     */
    public void tick() {
        if (!npc.isLookAtActive()) {
            return;
        }

        Vec3 targetPos = getTargetPosition();
        if (targetPos == null) {
            stop();
            return;
        }

        // Calculate target angles
        double dx = targetPos.x - npc.getX();
        double dy = targetPos.y - npc.getEyeY();
        double dz = targetPos.z - npc.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float targetPitch = (float) (-(Mth.atan2(dy, horizontalDist) * Mth.RAD_TO_DEG));

        // Clamp pitch to reasonable limits
        targetPitch = Mth.clamp(targetPitch, -85.0F, 85.0F);

        // Rotate both body and head to look at target
        npc.setYRot(targetYaw);
        npc.yBodyRot = targetYaw;
        npc.yHeadRot = targetYaw;
        npc.setXRot(targetPitch);

        // If once mode - deactivate after this tick
        if (!npc.isLookAtContinuous()) {
            npc.setLookAtActive(false);
        }
    }

    /**
     * Gets the current target position for look-at.
     * If targeting an entity, updates position from entity.
     * Returns null if entity target is no longer valid.
     */
    private Vec3 getTargetPosition() {
        int entityId = npc.getLookAtEntityId();
        if (entityId >= 0) {
            Entity target = npc.level().getEntity(entityId);
            if (target != null && target.isAlive()) {
                return new Vec3(target.getX(), target.getEyeY(), target.getZ());
            }
            return null;
        }
        // Static coordinates
        return new Vec3(
                npc.getLookAtTargetX(),
                npc.getLookAtTargetY(),
                npc.getLookAtTargetZ()
        );
    }

    /**
     * Checks if look-at is currently active.
     */
    public boolean isActive() {
        return npc.isLookAtActive();
    }
}
