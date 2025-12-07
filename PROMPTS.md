# PROMPTS: &5=B@8@>20=85 H8@>:8E NPC 8 >1@01>B:0 425@59

---

## 040G0 1.1: !>740BL 107>2K9 :;0AA PassageAnalyzer

### &5;L
!>740BL CB8;8B=K9 :;0AA 4;O 0=0;870 ?@>E>4>2 8 2KG8A;5=8O >?B8<0;L=KE :>>@48=0B F5=B@8@>20=8O H8@>:8E NPC.

### $09;
`src/main/java/dcs/jagermeistars/talesmaker/pathfinding/movement/PassageAnalyzer.java`

### "@51>20=8O
1. !>740BL final :;0AA A private :>=AB@C:B>@><
2. >1028BL 703;CH:8 4;O A;54CNI8E <5B>4>2:
   - `measurePassageWidth(MovementContext ctx, BlockPos pos, Direction.Axis axis, float entityHeight) -> int`
   - `calculateOptimalX(MovementContext ctx, BlockPos src, BlockPos dest, float entityWidth) -> double`
   - `calculateOptimalZ(MovementContext ctx, BlockPos src, BlockPos dest, float entityWidth) -> double`
   - `getDoorOffset(BlockState doorState, Direction movementDirection) -> double`
3. >1028BL JavaDoc :><<5=B0@88

### <?>@BK
```java
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
```

### @>25@:0
```bash
./gradlew build
```

---

## 040G0 1.2:  50;87>20BL measurePassageWidth

### &5;L
 50;87>20BL <5B>4 87<5@5=8O H8@8=K ?@>E>40 2 C:070==>< =0?@02;5=88.

### $09;
`src/main/java/dcs/jagermeistars/talesmaker/pathfinding/movement/PassageAnalyzer.java`

### ;3>@8B<
1. @8=OBL =0G0;L=CN ?>78F8N 8 >AL 0=0;870 (X 8;8 Z)
2. @>25@8BL headroom 2 =0G0;L=>9 ?>78F88 (5A;8 =5B - 25@=CBL 0)
3. B5@8@>20BL 2 ?>;>68B5;L=>< =0?@02;5=88 >A8, ?>:0 5ABL headroom (<0:A 5 1;>:>2)
4. B5@8@>20BL 2 >B@8F0B5;L=>< =0?@02;5=88 >A8, ?>:0 5ABL headroom (<0:A 5 1;>:>2)
5. 5@=CBL >1ICN H8@8=C (:>;8G5AB2> ?@>E>48<KE 1;>:>2)

### 5B>4
```java
/**
 * 7<5@O5B H8@8=C ?@>E>40 2 C:070==>< =0?@02;5=88.
 * @param ctx :>=B5:AB 42865=8O
 * @param pos ?>78F8O 4;O 0=0;870
 * @param axis >AL 4;O 87<5@5=8O (X 8;8 Z)
 * @param entityHeight 2KA>B0 NPC 4;O ?@>25@:8 headroom
 * @return H8@8=0 ?@>E>40 2 1;>:0E (<8=8<C< 1 5A;8 5ABL headroom, 8=0G5 0)
 */
public static int measurePassageWidth(MovementContext ctx, BlockPos pos, Direction.Axis axis, float entityHeight)
```

### Helper <5B>4
>1028BL private <5B>4 4;O A<5I5=8O BlockPos ?> >A8:
```java
private static BlockPos offsetByAxis(BlockPos pos, Direction.Axis axis, int offset) {
    return switch (axis) {
        case X -> pos.offset(offset, 0, 0);
        case Y -> pos.offset(0, offset, 0);
        case Z -> pos.offset(0, 0, offset);
    };
}
```

### @>25@:0
```bash
./gradlew build
```

---

## 040G0 1.3:  50;87>20BL calculateOptimalX 8 calculateOptimalZ

### &5;L
 50;87>20BL <5B>4K 2KG8A;5=8O >?B8<0;L=KE :>>@48=0B 4;O F5=B@8@>20=8O H8@>:8E NPC 2 ?@>E>40E.

### $09;
`src/main/java/dcs/jagermeistars/talesmaker/pathfinding/movement/PassageAnalyzer.java`

### ;3>@8B< 4;O calculateOptimalX
```java
public static double calculateOptimalX(MovementContext ctx, BlockPos src, BlockPos dest, float entityWidth) {
    // 1. A;8 NPC C7:89 - AB0=40@B=K9 F5=B@ 1;>:0
    if (entityWidth <= 1.0f) {
        return dest.getX() + 0.5;
    }

    // 2. ?@545;8BL =0?@02;5=85 42865=8O
    int dx = dest.getX() - src.getX();
    int dz = dest.getZ() - src.getZ();

    // 3. A;8 42865=85 ?> Z - 0=0;878@C5< ?@>E>4 ?> X
    if (Math.abs(dz) > Math.abs(dx)) {
        return calculateCenteredCoord(ctx, dest, Direction.Axis.X, entityWidth);
    }

    // 4. A;8 42865=85 ?> X - ?@>AB> F5=B@ 1;>:0 ?> X
    return dest.getX() + 0.5;
}
```

### ;3>@8B< 4;O calculateCenteredCoord (private helper)
```java
private static double calculateCenteredCoord(MovementContext ctx, BlockPos pos, Direction.Axis axis, float entityWidth) {
    float entityHeight = ctx.getConfig().getEntityHeight();

    // 09B8 3@0=8FK ?@>E>40
    int minOffset = 0;
    int maxOffset = 0;

    // @>25@8BL 2 >B@8F0B5;L=>< =0?@02;5=88
    for (int i = -1; i >= -5; i--) {
        BlockPos checkPos = offsetByAxis(pos, axis, i);
        if (MovementHelper.hasVerticalClearance(ctx, checkPos, entityHeight)) {
            minOffset = i;
        } else {
            break;
        }
    }

    // @>25@8BL 2 ?>;>68B5;L=>< =0?@02;5=88
    for (int i = 1; i <= 5; i++) {
        BlockPos checkPos = offsetByAxis(pos, axis, i);
        if (MovementHelper.hasVerticalClearance(ctx, checkPos, entityHeight)) {
            maxOffset = i;
        } else {
            break;
        }
    }

    // (8@8=0 ?@>E>40
    int passageWidth = maxOffset - minOffset + 1;

    // 07>20O :>>@48=0B0 1;>:0
    double baseCoord = (axis == Direction.Axis.X) ? pos.getX() : pos.getZ();

    // A;8 NPC H8@5 8;8 @025= ?@>E>4C - F5=B@8@>20BL 2 ?@>E>45
    if (entityWidth >= passageWidth - 0.1f) {
        return baseCoord + 0.5 + (minOffset + maxOffset) / 2.0;
    }

    // =0G5 AB0=40@B=K9 F5=B@ 1;>:0
    return baseCoord + 0.5;
}
```

### @8<5@ 4;O NPC width=2.0 2 ?@>E>45 2 1;>:0
- @>E>4 87 1;>:>2 X=5 8 X=6
- pos = (5, y, z), minOffset = 0, maxOffset = 1
- passageWidth = 2
- entityWidth (2.0) >= passageWidth (2)
- passageCenter = 5 + 0.5 + (0+1)/2 = 5.5 + 0.5 = 6.0
- NPC F5=B@8@C5BAO =0 X=6.0 (3@0=8F0 <564C 1;>:0<8 5 8 6)

### @>25@:0
```bash
./gradlew build
```

---

## 040G0 1.4:  50;87>20BL getDoorOffset

### &5;L
 50;87>20BL <5B>4 2KG8A;5=8O A<5I5=8O F5=B@0 ?@>E>40 87-70 E8B1>:A0 425@8.

### $09;
`src/main/java/dcs/jagermeistars/talesmaker/pathfinding/movement/PassageAnalyzer.java`

### =D>@<0F8O > 425@OE Minecraft
- ">;I8=0 E8B1>:A0 425@8: 3/16 1;>:0 (0.1875)
- FACING >?@545;O5B =0 :0:>9 AB>@>=5 1;>:0 =0E>48BAO 425@L:
  - NORTH: 425@L =0 Z+ AB>@>=5 1;>:0 (maxZ = 1.0, 425@L 70=8<05B 0.8125 - 1.0)
  - SOUTH: 425@L =0 Z- AB>@>=5 1;>:0 (minZ = 0.0, 425@L 70=8<05B 0.0 - 0.1875)
  - EAST: 425@L =0 X+ AB>@>=5 1;>:0
  - WEST: 425@L =0 X- AB>@>=5 1;>:0

### ;3>@8B<
```java
public static double getDoorOffset(BlockState doorState, Direction movementDirection) {
    if (!(doorState.getBlock() instanceof DoorBlock)) {
        return 0.0;
    }

    Direction facing = doorState.getValue(DoorBlock.FACING);
    double doorThickness = 3.0 / 16.0; // 0.1875
    double offset = doorThickness / 2.0; // ~0.09375

    // !<5I5=85 7028A8B >B =0?@02;5=8O 425@8 8 =0?@02;5=8O 42865=8O
    // A;8 42865=85 ?5@?5=48:C;O@=> 425@8 - 157 A<5I5=8O
    // A;8 42865=85 ?0@0;;5;L=> 425@8 - A<5I05<AO >B 425@8

    return switch (facing) {
        case NORTH -> (movementDirection.getAxis() == Direction.Axis.Z) ? offset : 0.0;
        case SOUTH -> (movementDirection.getAxis() == Direction.Axis.Z) ? -offset : 0.0;
        case EAST -> (movementDirection.getAxis() == Direction.Axis.X) ? -offset : 0.0;
        case WEST -> (movementDirection.getAxis() == Direction.Axis.X) ? offset : 0.0;
        default -> 0.0;
    };
}
```

### <?>@BK
```java
import net.minecraft.world.level.block.DoorBlock;
```

### @>25@:0
```bash
./gradlew build
```

---

## 040G0 2.1: >1028BL calculateTargetPosition 2 MovementHelper

### &5;L
>1028BL C=8D8F8@>20==K9 <5B>4 4;O 2KG8A;5=8O F5;52>9 ?>78F88 42865=8O A CG5B>< H8@8=K NPC.

### $09;
`src/main/java/dcs/jagermeistars/talesmaker/pathfinding/movement/MovementHelper.java`

### 5B>4
```java
/**
 * KG8A;O5B >?B8<0;L=CN F5;52CN ?>78F8N 4;O NPC A CG5B>< 53> H8@8=K.
 * ;O H8@>:8E NPC F5=B@8@C5B 2 ?@>E>45, 4;O C7:8E - ?> F5=B@C 1;>:0.
 *
 * @param ctx :>=B5:AB 42865=8O
 * @param src 8AE>4=0O ?>78F8O
 * @param dest F5;520O ?>78F8O
 * @return Vec3 A >?B8<0;L=K<8 :>>@48=0B0<8
 */
public static Vec3 calculateTargetPosition(MovementContext ctx, BlockPos src, BlockPos dest) {
    float entityWidth = ctx.getConfig().getEntityWidth();

    double x = PassageAnalyzer.calculateOptimalX(ctx, src, dest, entityWidth);
    double z = PassageAnalyzer.calculateOptimalZ(ctx, src, dest, entityWidth);
    double y = dest.getY();

    return new Vec3(x, y, z);
}
```

### 5@53@C7:0 4;O 425@59
```java
/**
 * KG8A;O5B F5;52CN ?>78F8N A CG5B>< E8B1>:A0 425@8.
 */
public static Vec3 calculateTargetPositionThroughDoor(
    MovementContext ctx,
    BlockPos src,
    BlockPos dest,
    BlockPos doorPos
) {
    Vec3 baseTarget = calculateTargetPosition(ctx, src, dest);

    BlockState doorState = ctx.getBlockState(doorPos);
    Direction moveDir = getMovementDirection(src, dest);
    double doorOffset = PassageAnalyzer.getDoorOffset(doorState, moveDir);

    if (moveDir.getAxis() == Direction.Axis.X) {
        return baseTarget.add(0, 0, doorOffset);
    } else {
        return baseTarget.add(doorOffset, 0, 0);
    }
}

private static Direction getMovementDirection(BlockPos from, BlockPos to) {
    int dx = to.getX() - from.getX();
    int dz = to.getZ() - from.getZ();
    if (Math.abs(dx) > Math.abs(dz)) {
        return dx > 0 ? Direction.EAST : Direction.WEST;
    } else {
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
```

### @>25@:0
```bash
./gradlew build
```

---

## 040G0 3.1: 1=>28BL MovementTraverse

### &5;L
0<5=8BL D8:A8@>20==>5 F5=B@8@>20=85 =0 2KG8A;5=85 >?B8<0;L=>9 ?>78F88.

### $09;
`src/main/java/dcs/jagermeistars/talesmaker/pathfinding/movement/movements/MovementTraverse.java`

### 7<5=5=8O
1. 09B8 AB@>:C 2 <5B>45 `tick()`:
```java
Vec3 targetPos = Vec3.atBottomCenterOf(dest);
```

2. 0<5=8BL =0:
```java
Vec3 targetPos = MovementHelper.calculateTargetPosition(ctx, src, dest);
```

### @>25@:0
```bash
./gradlew build
```

---

## 040G0 3.2: 1=>28BL MovementDiagonal

### &5;L
1=>28BL 4803>=0;L=>5 42865=85 4;O ?>445@6:8 H8@>:8E NPC.

### $09;
`src/main/java/dcs/jagermeistars/talesmaker/pathfinding/movement/movements/MovementDiagonal.java`

### 7<5=5=8O
0<5=8BL:
```java
Vec3 targetPos = Vec3.atBottomCenterOf(dest);
```

0:
```java
Vec3 targetPos = MovementHelper.calculateTargetPosition(ctx, src, dest);
```

### @>25@:0
```bash
./gradlew build
```

---

## 040G0 3.3: 1=>28BL MovementDoor

### &5;L
1=>28BL 42865=85 G5@57 425@L A CG5B>< E8B1>:A0 425@8.

### $09;
`src/main/java/dcs/jagermeistars/talesmaker/pathfinding/movement/movements/MovementDoor.java`

### 7<5=5=8O

1.  A>AB>O=88 WAITING - 42865=85 : 425@8 A CG5B>< A<5I5=8O:
```java
// K;>:
Vec3 doorCenter = Vec3.atCenterOf(doorPos);

// !B0;>:
Direction moveDir = getMovementDirection(src, dest);
BlockState doorState = ctx.getBlockState(doorPos);
double doorOffset = PassageAnalyzer.getDoorOffset(doorState, moveDir);
Vec3 doorCenter = Vec3.atCenterOf(doorPos);
if (moveDir.getAxis() == Direction.Axis.X) {
    doorCenter = doorCenter.add(0, 0, doorOffset);
} else {
    doorCenter = doorCenter.add(doorOffset, 0, 0);
}
```

2.  A>AB>O=88 RUNNING - ?@>E>4 G5@57 425@L:
```java
// K;>:
Vec3 targetPos = Vec3.atBottomCenterOf(dest);

// !B0;>:
Vec3 targetPos = MovementHelper.calculateTargetPositionThroughDoor(ctx, src, dest, doorPos);
```

3. >1028BL helper <5B>4:
```java
private Direction getMovementDirection(BlockPos from, BlockPos to) {
    int dx = to.getX() - from.getX();
    int dz = to.getZ() - from.getZ();
    if (Math.abs(dx) > Math.abs(dz)) {
        return dx > 0 ? Direction.EAST : Direction.WEST;
    } else {
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
```

### <?>@BK
```java
import dcs.jagermeistars.talesmaker.pathfinding.movement.PassageAnalyzer;
import net.minecraft.core.Direction;
```

### @>25@:0
```bash
./gradlew build
```

---

## 040G0 3.4: 1=>28BL MovementAscend 8 MovementDescend

### &5;L
1=>28BL 25@B8:0;L=K5 42865=8O 4;O ?>445@6:8 H8@>:8E NPC.

### $09;K
- `src/main/java/dcs/jagermeistars/talesmaker/pathfinding/movement/movements/MovementAscend.java`
- `src/main/java/dcs/jagermeistars/talesmaker/pathfinding/movement/movements/MovementDescend.java`

### 7<5=5=8O
 :064>< D09;5 70<5=8BL:
```java
Vec3 targetPos = Vec3.atBottomCenterOf(dest);
```

0:
```java
Vec3 targetPos = MovementHelper.calculateTargetPosition(ctx, src, dest);
```

### @>25@:0
```bash
./gradlew build
```

---

## 040G0 3.5: 1=>28BL >AB0;L=K5 Movement :;0AAK

### &5;L
1=>28BL >AB02H85AO Movement :;0AAK 4;O ?>445@6:8 H8@>:8E NPC.

### $09;K
- `MovementParkour.java`
- `MovementFall.java`
- `MovementSwim.java`

### 7<5=5=8O
 :064>< D09;5 70<5=8BL:
```java
Vec3 targetPos = Vec3.atBottomCenterOf(dest);
```

0:
```java
Vec3 targetPos = MovementHelper.calculateTargetPosition(ctx, src, dest);
```

### @8<5G0=85
;O `MovementPillar.java` (G8AB> 25@B8:0;L=>5 42865=85 225@E/2=87) <>6=> >AB028BL `atBottomCenterOf`, B0: :0: ?@8 25@B8:0;L=>< 42865=88 157 3>@87>=B0;L=>3> A<5I5=8O =5B =5>1E>48<>AB8 0=0;878@>20BL ?@>E>4.

### @>25@:0
```bash
./gradlew build
./gradlew runClient
```

---

## 040G0 4.1: "5AB8@>20=85 A8AB5<K F5=B@8@>20=8O

### &5;L
#1548BLAO GB> A8AB5<0 F5=B@8@>20=8O @01>B05B :>@@5:B=> 4;O 2A5E A;CG052.

### "5AB>2K5 AF5=0@88

#### !F5=0@89 1: NPC width=0.6 (AB0=40@B=K9)
1. !>740BL NPC A H8@8=>9 0.6
2. @>E>4 H8@8=>9 1 1;>:
3. **6840=85**: NPC 845B ?> F5=B@C 1;>:0 (x+0.5)

#### !F5=0@89 2: NPC width=2.0 2 ?@>E>45 2 1;>:0
1. !>740BL NPC A H8@8=>9 2.0
2. >AB@>8BL AB5=C A ?@>E>4>< H8@8=>9 2 1;>:0
3. **6840=85**: NPC F5=B@8@C5BAO =0 3@0=8F5 1;>:>2 (<564C 1;>:0<8 0 8 1)

#### !F5=0@89 3: NPC width=2.0 2 ?@>E>45 3 1;>:0
1. !>740BL NPC A H8@8=>9 2.0
2. @>E>4 H8@8=>9 3 1;>:0
3. **6840=85**: NPC <>65B 84B8 ?> F5=B@C A@54=53> 1;>:0

#### !F5=0@89 4: 25@L NORTH
1. NPC ?@>E>48B G5@57 425@L A FACING=NORTH
2. **6840=85**: NPC A<5I05BAO : N3C >B F5=B@0 (Z+0.09)

#### !F5=0@89 5: 25@L SOUTH
1. NPC ?@>E>48B G5@57 425@L A FACING=SOUTH
2. **6840=85**: NPC A<5I05BAO : A525@C >B F5=B@0 (Z-0.09)

### ><0=4K B5AB8@>20=8O
```bash
./gradlew runClient
```

### Debug 2K2>4 (2@5<5==K9)
>1028BL 2 `MovementHelper.calculateTargetPosition()`:
```java
System.out.println("[DEBUG] calculateTargetPosition: src=" + src +
    ", dest=" + dest +
    ", width=" + entityWidth +
    ", result=(" + x + ", " + y + ", " + z + ")");
```

#40;8BL ?>A;5 B5AB8@>20=8O!
