# Claude Code Instructions - TalesMaker NPC Animation System

## Overview
 5D0:B>@8=3 A8AB5<K 0=8<0F89 NPC =0 data-driven 0@E8B5:BC@C A GeckoLib 4.8.2.

---

## /",+  

### 1. 5@54 =0G0;>< @01>BK
```
! G8B09 TODO.md ?5@54 2K?>;=5=85< ;N1>9 7040G8!
```

### 2. >@O4>: @01>BK
1. B:@>9 8 ?@>G8B09 `TODO.md`
2. 0948 ?5@2CN =52K?>;=5==CN 7040GC `[ ]`
3. K?>;=8 ", MBC 7040GC
4. >A;5 7025@H5=8O >B<5BL `[x]` 2 `TODO.md`
5. 1=>28 Progress Summary 2 `TODO.md`
6. @>25@L :><?8;OF8N `./gradlew build`
7. 5@5E>48 : A;54CNI59 7040G5

### 3. @028;0 2K?>;=5=8O
- **4=0 7040G0 70 @07** - =5 2K?>;=O9 =5A:>;L:> 7040G A@07C
- **@>25@O9 7028A8<>AB8** - C1548AL GB> ?@54K4CI85 7040G8 2K?>;=5=K
- **"5AB8@C9 :><?8;OF8N** - ?>A;5 :064>3> 87<5=5=8O ?@>25@L `./gradlew build`
- **5 ?@>?CA:09 7040G8** - 2K?>;=O9 AB@>3> ?> ?>@O4:C

---

## "5:CI0O 7040G0: NPC Animation System Refactoring

### "@51>20=8O
1. **Data-driven** - 0=8<0F88 =0AB@0820NBAO G5@57 JSON preset
2. **!;>8 0=8<0F89**:
   - Base layer (idle/walk/run)
   - Action layer (attack, interact, emotes)
   - Override layer (death, stun)
3. **Head tracking** - @01>B05B ! (:@><5 override A blockHead)
4. **Turn animation** - 0=8<0F8O ?>2>@>B0 ?@8 @57:>< 87<5=5=88 =0?@02;5=8O
5. **#A;>2=K5 0=8<0F88** - variants 2 7028A8<>AB8 >B A>AB>O=8O
6. **;02=K5 transitions** A =0AB@08205<>9 4;8B5;L=>ABLN

### @E8B5:BC@0
```
[NpcAnimationConfig]     - Data (JSON preset)
        “
[NpcAnimationState]      - Runtime state (minimal synced data)
        “
[NpcAnimationManager]    - Server-side ;>38:0 2K1>@0 0=8<0F89
        “
[AnimationController]    - GeckoLib controller (client-side)
        “
[NpcModel]               - Additive head rotation (client-side)
```

### "5:CI85 D09;K
- `entity/NpcEntity.java` - >A=>2=>9 :;0AA NPC
- `client/model/NpcModel.java` - GeckoLib <>45;L
- `data/NpcPreset.java` - :>=D83C@0F8O NPC
- `entity/NpcLookHandler.java` - A8AB5<0 look-at

### >2K5 D09;K
- `data/NpcAnimationConfig.java` - data records + Codec
- `entity/NpcAnimationState.java` - runtime state
- `entity/NpcAnimationManager.java` - server-side ;>38:0

### >2K9 JSON D>@<0B
```json
{
  "animations": {
    "path": "talesmaker:animations/npc/guard.animation.json",
    "layers": {
      "base": {
        "idle": { "default": "idle", "variants": { "injured": "idle_hurt" } },
        "walk": { "default": "walk" },
        "run": { "default": "run", "threshold": 0.15 }
      },
      "action": {
        "attack": { "name": "attack", "mode": "once" }
      },
      "override": {
        "death": { "name": "death", "duration": 40, "blockHead": true }
      }
    },
    "transitions": { "default": 5 },
    "headTracking": {
      "enabled": true, "bone": "head", "mode": "additive",
      "maxYaw": 70, "maxPitch": 40
    },
    "bodyTurn": {
      "enabled": true, "threshold": 45, "animation": "turn"
    }
  }
}
```

### ;NG52>5 @5H5=85: Additive Head Tracking
```java
//  NpcModel.setCustomAnimations():
if ("additive".equals(headConfig.mode())) {
    // ADDITIVE: >102;O5< : B5:CI59 0=8<0F88, 0 =5 70<5=O5<
    headBone.setRotX(headBone.getRotX() + pitch * Mth.DEG_TO_RAD);
    headBone.setRotY(headBone.getRotY() + yaw * Mth.DEG_TO_RAD);
}
```

---

## !B@C:BC@0 ?@>5:B0

### 5@A88 8 7028A8<>AB8
- Minecraft: 1.21.1
- NeoForge: 21.1.215
- Java: 21
- GeckoLib: 4.8.2

---

## ><0=4K 4;O ?@>25@:8

### ><?8;OF8O
```bash
./gradlew build
```

### 0?CA: :;85=B0 4;O B5AB8@>20=8O
```bash
./gradlew runClient
```

---

## @8<5@ workflow

```
User: @>4>;68 @01>BC =04 animation system

Claude:
1. '8B0N TODO.md...
2. !;54CNI0O 7040G0: 1.1 !>740BL NpcAnimationConfig.java
3. !>740N D09; NpcAnimationConfig.java
4. @>25@ON :><?8;OF8N ./gradlew build
5. B<5G0N [x] 1.1 2 TODO.md
6. 1=>2;ON Progress Summary

040G0 1.1 2K?>;=5=0. !;54CNI0O: 1.2 1=>28BL NpcPreset.java
```

---

## >=B0:BK 8 @5AC@AK

- NeoForge Docs: https://docs.neoforged.net/
- GeckoLib Wiki: https://github.com/bernie-g/geckolib/wiki
- ;0= ?@>5:B0: `.claude/plans/sleepy-snuggling-moth.md`
