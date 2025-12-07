package dcs.jagermeistars.talesmaker.pathfinding.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Utility class for analyzing passages and calculating optimal centering coordinates
 * for wide NPCs navigating through corridors and doorways.
 */
public final class PassageAnalyzer {

    private PassageAnalyzer() {
        // Utility class - no instantiation
    }

    /**
     * Measures the width of a passage in the specified direction.
     *
     * @param ctx          movement context
     * @param pos          position to analyze
     * @param axis         axis to measure along (X or Z)
     * @param entityHeight entity height for headroom checks
     * @return passage width in blocks (minimum 1 if passable, 0 if blocked)
     */
    public static int measurePassageWidth(MovementContext ctx, BlockPos pos, Direction.Axis axis, float entityHeight) {
        // Check if the starting position has headroom
        if (!MovementHelper.hasVerticalClearance(ctx, pos, entityHeight)) {
            return 0;
        }

        int width = 1; // Start with the current block

        // Check in positive direction (max 5 blocks)
        for (int i = 1; i <= 5; i++) {
            BlockPos checkPos = offsetByAxis(pos, axis, i);
            if (MovementHelper.hasVerticalClearance(ctx, checkPos, entityHeight)) {
                width++;
            } else {
                break;
            }
        }

        // Check in negative direction (max 5 blocks)
        for (int i = 1; i <= 5; i++) {
            BlockPos checkPos = offsetByAxis(pos, axis, -i);
            if (MovementHelper.hasVerticalClearance(ctx, checkPos, entityHeight)) {
                width++;
            } else {
                break;
            }
        }

        return width;
    }

    /**
     * Calculates the optimal X coordinate for centering a wide NPC in a passage.
     * For narrow NPCs (width <= 1.0), returns the standard block center.
     * For wide NPCs, calculates the center of the passage to avoid wall collisions.
     *
     * @param ctx         movement context
     * @param src         source position
     * @param dest        destination position
     * @param entityWidth entity width
     * @return optimal X coordinate in world space
     */
    public static double calculateOptimalX(MovementContext ctx, BlockPos src, BlockPos dest, float entityWidth) {
        // For narrow NPCs, use standard block center
        if (entityWidth <= 1.0f) {
            return dest.getX() + 0.5;
        }

        // For wide NPCs, ALWAYS center in passage along X axis
        // This ensures the NPC doesn't clip into walls regardless of movement direction
        return calculateCenteredCoord(ctx, dest, Direction.Axis.X, entityWidth);
    }

    /**
     * Calculates the optimal Z coordinate for centering a wide NPC in a passage.
     * For narrow NPCs (width <= 1.0), returns the standard block center.
     * For wide NPCs, calculates the center of the passage to avoid wall collisions.
     *
     * @param ctx         movement context
     * @param src         source position
     * @param dest        destination position
     * @param entityWidth entity width
     * @return optimal Z coordinate in world space
     */
    public static double calculateOptimalZ(MovementContext ctx, BlockPos src, BlockPos dest, float entityWidth) {
        // For narrow NPCs, use standard block center
        if (entityWidth <= 1.0f) {
            return dest.getZ() + 0.5;
        }

        // For wide NPCs, ALWAYS center in passage along Z axis
        // This ensures the NPC doesn't clip into walls regardless of movement direction
        return calculateCenteredCoord(ctx, dest, Direction.Axis.Z, entityWidth);
    }

    /**
     * Calculates the centered coordinate for a wide NPC in a passage.
     *
     * For an NPC of width W to fit in a passage without touching walls,
     * it needs to be centered so that W/2 on each side doesn't hit the walls.
     *
     * Example: NPC width=2.0 in a 2-block passage (blocks at x=0 and x=1)
     * - Passage spans from x=0 to x=2 (total width 2.0)
     * - NPC center must be at x=1.0 (the boundary between blocks)
     * - This way NPC extends from x=0 to x=2, exactly filling the passage
     *
     * @param ctx         movement context
     * @param pos         position to analyze
     * @param axis        axis of the passage (perpendicular to movement)
     * @param entityWidth entity width
     * @return optimal coordinate along the specified axis
     */
    private static double calculateCenteredCoord(MovementContext ctx, BlockPos pos, Direction.Axis axis, float entityWidth) {
        float entityHeight = ctx.getConfig().getEntityHeight();

        // Find passage boundaries (first solid block on each side)
        int minOffset = 0;  // Last passable block in negative direction
        int maxOffset = 0;  // Last passable block in positive direction

        // Check in negative direction - find the wall
        for (int i = -1; i >= -5; i--) {
            BlockPos checkPos = offsetByAxis(pos, axis, i);
            if (MovementHelper.hasVerticalClearance(ctx, checkPos, entityHeight)) {
                minOffset = i;
            } else {
                break;
            }
        }

        // Check in positive direction - find the wall
        for (int i = 1; i <= 5; i++) {
            BlockPos checkPos = offsetByAxis(pos, axis, i);
            if (MovementHelper.hasVerticalClearance(ctx, checkPos, entityHeight)) {
                maxOffset = i;
            } else {
                break;
            }
        }

        // Get base coordinate of the destination block
        double baseCoord = (axis == Direction.Axis.X) ? pos.getX() : pos.getZ();

        // Calculate actual passage boundaries in world coordinates
        // Wall on negative side is at (baseCoord + minOffset), so passage starts at (baseCoord + minOffset)
        // Wall on positive side is at (baseCoord + maxOffset + 1), so passage ends at (baseCoord + maxOffset + 1)
        double passageStart = baseCoord + minOffset;
        double passageEnd = baseCoord + maxOffset + 1;
        double passageWidth = passageEnd - passageStart;

        // Calculate center of passage
        double passageCenter = (passageStart + passageEnd) / 2.0;

        // DEBUG logging
        System.out.println("[PassageAnalyzer] axis=" + axis + " pos=" + pos + " entityWidth=" + entityWidth + " entityHeight=" + entityHeight);
        System.out.println("[PassageAnalyzer] minOffset=" + minOffset + " maxOffset=" + maxOffset);
        System.out.println("[PassageAnalyzer] passageStart=" + passageStart + " passageEnd=" + passageEnd + " passageWidth=" + passageWidth);
        System.out.println("[PassageAnalyzer] passageCenter=" + passageCenter + " standardCenter=" + (baseCoord + 0.5));

        // For wide NPCs (width > 1.0), always center in passage
        // This ensures NPC doesn't clip into walls
        // For narrower passages or when NPC barely fits, centering is critical
        if (entityWidth > 1.0f) {
            System.out.println("[PassageAnalyzer] Using passageCenter: " + passageCenter);
            return passageCenter;
        }

        // For narrow NPCs, use standard block center
        System.out.println("[PassageAnalyzer] Using standardCenter: " + (baseCoord + 0.5));
        return baseCoord + 0.5;
    }

    /**
     * Calculates the offset from block center due to door hitbox.
     * Doors have a hitbox of approximately 3/16 blocks (0.1875),
     * which shifts the passable center away from the door panel.
     *
     * @param doorState         block state of the door
     * @param movementDirection direction of movement through the door
     * @return offset in blocks (positive or negative depending on door facing)
     */
    public static double getDoorOffset(BlockState doorState, Direction movementDirection) {
        if (!(doorState.getBlock() instanceof DoorBlock)) {
            return 0.0;
        }

        Direction facing = doorState.getValue(DoorBlock.FACING);
        double doorThickness = 3.0 / 16.0; // 0.1875 blocks
        double offset = doorThickness / 2.0; // ~0.09375

        // Offset depends on door facing and movement direction
        // When movement is perpendicular to door, no offset needed
        // When movement is parallel to door facing, offset away from door panel
        return switch (facing) {
            case NORTH -> (movementDirection.getAxis() == Direction.Axis.Z) ? offset : 0.0;
            case SOUTH -> (movementDirection.getAxis() == Direction.Axis.Z) ? -offset : 0.0;
            case EAST -> (movementDirection.getAxis() == Direction.Axis.X) ? -offset : 0.0;
            case WEST -> (movementDirection.getAxis() == Direction.Axis.X) ? offset : 0.0;
            default -> 0.0;
        };
    }

    /**
     * Helper method to offset a BlockPos along a specific axis.
     *
     * @param pos    original position
     * @param axis   axis to offset along
     * @param offset amount to offset (can be negative)
     * @return new BlockPos offset along the specified axis
     */
    private static BlockPos offsetByAxis(BlockPos pos, Direction.Axis axis, int offset) {
        return switch (axis) {
            case X -> pos.offset(offset, 0, 0);
            case Y -> pos.offset(0, offset, 0);
            case Z -> pos.offset(0, 0, offset);
        };
    }
}
