package dcs.jagermeistars.talesmaker.pathfinding.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/**
 * Utility class for analyzing passages and calculating optimal centering coordinates
 * for wide NPCs navigating through corridors and doorways.
 */
public final class PassageAnalyzer {

    /**
     * Width of passage through an open door in blocks.
     * Door hitbox leaves only 0.7 blocks of passable space when open.
     */
    public static final double DOOR_PASSAGE_WIDTH = 0.7;

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

        // Calculate center of passage
        double passageCenter = (passageStart + passageEnd) / 2.0;

        // For wide NPCs (width > 1.0), always center in passage
        if (entityWidth > 1.0f) {
            return passageCenter;
        }

        // For narrow NPCs, use standard block center
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
     * Calculates the center position for NPC to pass through a door.
     * When a door is open, it swings to one side (based on hinge position),
     * leaving the passage on the OPPOSITE side from where the door swings.
     *
     * Door thickness is 3/16 blocks (0.1875). When open, the door rotates 90 degrees.
     *
     * @param doorPos     position of the door block
     * @param doorState   block state of the door
     * @param entityWidth width of the entity
     * @return Vec3 with X and Z coordinates for passing through the door
     */
    public static double[] calculateDoorPassagePosition(BlockPos doorPos, BlockState doorState, float entityWidth) {
        double baseX = doorPos.getX() + 0.5;
        double baseZ = doorPos.getZ() + 0.5;

        if (!(doorState.getBlock() instanceof DoorBlock)) {
            return new double[]{baseX, baseZ};
        }

        // Door thickness: 3/16 blocks = 0.1875
        double doorThickness = 3.0 / 16.0;

        // Get door properties
        Direction facing = doorState.getValue(DoorBlock.FACING);
        DoorHingeSide hinge = doorState.getValue(DoorBlock.HINGE);
        boolean isOpen = doorState.getValue(DoorBlock.OPEN);

        // If door is closed, use block center
        if (!isOpen) {
            return new double[]{baseX, baseZ};
        }

        // When door is open, it swings perpendicular to its facing direction
        // NORTH/SOUTH facing doors swing along X axis
        // EAST/WEST facing doors swing along Z axis

        // Calculate where the door panel is and offset NPC away from it
        double offsetAmount = (1.0 - doorThickness) / 2.0 - 0.5; // ~-0.09

        double resultX = baseX;
        double resultZ = baseZ;

        switch (facing) {
            case NORTH:
                // Door swings along X axis
                if (hinge == DoorHingeSide.LEFT) {
                    // Hinge on west (-X), door swings to west, passage on east (+X)
                    resultX = doorPos.getX() + doorThickness + (1.0 - doorThickness) / 2.0;
                } else {
                    // Hinge on east (+X), door swings to east, passage on west (-X)
                    resultX = doorPos.getX() + (1.0 - doorThickness) / 2.0;
                }
                break;
            case SOUTH:
                // Door swings along X axis
                if (hinge == DoorHingeSide.LEFT) {
                    // Hinge on east (+X), door swings to east, passage on west (-X)
                    resultX = doorPos.getX() + (1.0 - doorThickness) / 2.0;
                } else {
                    // Hinge on west (-X), door swings to west, passage on east (+X)
                    resultX = doorPos.getX() + doorThickness + (1.0 - doorThickness) / 2.0;
                }
                break;
            case EAST:
                // Door swings along Z axis
                if (hinge == DoorHingeSide.LEFT) {
                    // Hinge on north (-Z), door swings to north, passage on south (+Z)
                    resultZ = doorPos.getZ() + doorThickness + (1.0 - doorThickness) / 2.0;
                } else {
                    // Hinge on south (+Z), door swings to south, passage on north (-Z)
                    resultZ = doorPos.getZ() + (1.0 - doorThickness) / 2.0;
                }
                break;
            case WEST:
                // Door swings along Z axis
                if (hinge == DoorHingeSide.LEFT) {
                    // Hinge on south (+Z), door swings to south, passage on north (-Z)
                    resultZ = doorPos.getZ() + (1.0 - doorThickness) / 2.0;
                } else {
                    // Hinge on north (-Z), door swings to north, passage on south (+Z)
                    resultZ = doorPos.getZ() + doorThickness + (1.0 - doorThickness) / 2.0;
                }
                break;
            default:
                break;
        }

        return new double[]{resultX, resultZ};
    }

    /**
     * Legacy method for backward compatibility.
     * @deprecated Use calculateDoorPassagePosition instead
     */
    @Deprecated
    public static double calculateDoorPassageCenter(BlockPos doorPos, BlockState doorState,
                                                     Direction.Axis passageAxis, float entityWidth) {
        double[] pos = calculateDoorPassagePosition(doorPos, doorState, entityWidth);
        return (passageAxis == Direction.Axis.X) ? pos[0] : pos[1];
    }

    /**
     * Checks if a position contains a door block.
     *
     * @param ctx movement context
     * @param pos position to check
     * @return true if position contains a door
     */
    public static boolean isDoorAt(MovementContext ctx, BlockPos pos) {
        BlockState state = ctx.getBlockState(pos);
        return state.getBlock() instanceof DoorBlock;
    }

    /**
     * Checks if an entity with given width can pass through a door.
     *
     * @param entityWidth width of the entity
     * @return true if entity can fit through door passage
     */
    public static boolean canFitThroughDoor(float entityWidth) {
        return entityWidth <= DOOR_PASSAGE_WIDTH;
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
