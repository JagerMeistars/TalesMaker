# TalesMaker

A Minecraft NeoForge mod (1.21.1) for creating custom NPCs with animations, dialogue systems, and scriptable behaviors. Built with GeckoLib for animations.

## Project Overview

- **Mod ID**: `talesmaker`
- **Version**: 1.3.0
- **Minecraft Version**: 1.21.1
- **NeoForge Version**: 21.1.215
- **Java Version**: 21
- **Package**: `dcs.jagermeistars.talesmaker`

## Build & Development

```bash
# Build the mod
./gradlew build

# Run Minecraft client with mod
./gradlew runClient

# Run Minecraft server with mod
./gradlew runServer

# Generate data (resources)
./gradlew runData

# Refresh dependencies
./gradlew --refresh-dependencies

# Clean build
./gradlew clean
```

## Project Structure

```
src/main/java/dcs/jagermeistars/talesmaker/
├── TalesMaker.java              # Main mod class (server-side)
├── TalesMakerClient.java        # Client-side initialization
├── entity/
│   ├── NpcEntity.java           # Core NPC entity with all state
│   ├── NpcMovementHandler.java  # Movement behaviors (goto, patrol, follow, wander)
│   └── NpcLookHandler.java      # Look-at behavior system
├── client/
│   ├── model/NpcModel.java      # GeckoLib model integration
│   ├── renderer/NpcRenderer.java
│   ├── dialogue/                # Dialogue UI system
│   │   ├── DialogueManager.java
│   │   ├── DialogueOverlay.java
│   │   ├── DialogueHistory.java
│   │   └── DialogueHistoryScreen.java
│   ├── notification/            # In-game notifications
│   └── interact/                # Interact hint overlay
├── command/
│   └── TalesMakerCommands.java  # All /talesmaker commands
├── network/                     # Client-server packets
│   ├── ModNetworking.java       # Packet registration
│   ├── DialoguePacket.java
│   ├── NotificationPacket.java
│   └── ...
├── data/
│   ├── NpcPreset.java           # NPC preset data structure
│   └── NpcPresetManager.java    # Preset loading/management
├── init/
│   └── ModEntities.java         # Entity type registration
└── monologue/                   # Player monologue system
    ├── MonologueManager.java
    └── MonologueChatHandler.java

src/main/resources/
├── assets/talesmaker/
│   ├── geo/entity/              # GeckoLib models (.geo.json)
│   ├── textures/entity/         # Entity textures
│   ├── animations/entity/       # GeckoLib animations (.animation.json)
│   └── lang/                    # Localization (en_us.json, ru_ru.json)
├── data/talesmaker/
│   └── npc/presets/             # NPC preset definitions (.json)
└── META-INF/
    └── neoforge.mods.toml       # Mod metadata
```

## Key Systems

### NPC Entity (`NpcEntity.java`)

The core entity class with:
- **Synched Data**: All NPC state is synchronized via `EntityDataAccessor` fields
- **Preset System**: Visual configuration (model, texture, animations) via JSON presets
- **Script System**: Event-based commands (default, interact, player_nearby)
- **Movement System**: Multiple movement modes (goto, patrol, follow, wander, directional)
- **Look-at System**: Target-based rotation (coordinates or entity, once/continuous)
- **Custom Animations**: Play animations with modes (once, loop, hold)
- **GeckoLib Integration**: Implements `GeoEntity` for animated models

### NPC Presets

JSON files in `data/talesmaker/npc/presets/` defining NPC appearance:

```json
{
    "id": "example",
    "name": {"text": "Example NPC", "color": "gold"},
    "model": "talesmaker:geo/entity/example.geo.json",
    "texture": "talesmaker:textures/entity/example.png",
    "emissive": "talesmaker:textures/entity/example_emissive.png",  // optional
    "icon": "talesmaker:textures/entity/example_icon.png",
    "animations": {
        "path": "talesmaker:animations/entity/example.animation.json",
        "idle": "animation.example.idle",
        "walk": "animation.example.walk",
        "death": {"name": "animation.example.death", "duration": 40}
    },
    "head": "head",  // bone name for look rotation
    "hitbox": {"width": 0.6, "height": 1.8}
}
```

### Commands

All commands require permission level 2 (operator). Main command: `/talesmaker`

**NPC Management:**
- `/talesmaker npc create <pos> <id> <preset> [invulnerable] [script_type] [command]`
- `/talesmaker npc remove <id>`
- `/talesmaker npc list`
- `/talesmaker npc info <id>`
- `/talesmaker npc set script <id> <type> <command>`
- `/talesmaker npc set preset <id> <preset>`
- `/talesmaker npc set invulnerable <id> <true|false>`

**Movement:**
- `/talesmaker movement goto <id> <pos|entity>`
- `/talesmaker movement stop <id>`
- `/talesmaker movement patrol <id> <pos1> <pos2> [pos3] [pos4] [pos5]`
- `/talesmaker movement follow <id> <entity>`
- `/talesmaker movement forward|backward|left|right <id> <distance>`
- `/talesmaker movement wander <id> <radius>` or `<center> <radius>`

**Rotation:**
- `/talesmaker rotate start <id> <pos|entity> <once|continuous>`
- `/talesmaker rotate stop <id>`

**Animation:**
- `/talesmaker anim play <id> <animation> [once|loop|hold]`
- `/talesmaker anim stop <id>`

**Dialogue:**
- `/talesmaker dialogue say <npc_id> <message>` or `<entity_selector> <message>`
- `/talesmaker dialogue times <duration_ticks>`
- `/talesmaker history clear`

**Monologue:**
- `/talesmaker monologue enable <preset>`
- `/talesmaker monologue disable`

### Script Types

NPC scripts execute commands with placeholder replacement:
- `{npc}` - NPC's custom ID
- `{player}` - Triggering player's name
- `{x}`, `{y}`, `{z}` - NPC's coordinates

**Script Types:**
- `default` - Executes immediately when NPC spawns
- `interact` - Executes when player presses interact key (X) while looking at NPC
- `player_nearby <radius>` - Executes when player enters radius (one-time trigger)

### Networking

Packets use NeoForge's `PayloadRegistrar`:
- `DialoguePacket` - Display dialogue overlay (server -> client)
- `DialogueTimesPacket` - Set dialogue duration (server -> client)
- `NotificationPacket` - Show notification (server -> client)
- `InteractScriptPacket` - Trigger interact script (client -> server)
- `ClearHistoryPacket` - Clear dialogue history (server -> client)
- `ReloadNotifyPacket` - Notify preset reload (server -> client)

### Client Key Bindings

- `H` - Open dialogue history screen
- `X` - Interact with NPC (triggers interact script)

## Code Conventions

### Entity Data Synchronization

All NPC state that needs client sync uses `EntityDataAccessor`:
```java
private static final EntityDataAccessor<String> CUSTOM_ID = SynchedEntityData.defineId(
    NpcEntity.class, EntityDataSerializers.STRING);
```

### Command Implementation Pattern

Commands follow this structure in `TalesMakerCommands.java`:
1. Get arguments from context
2. Validate server level
3. Find NPC by custom ID using `serverLevel.getEntities()`
4. Perform action
5. Send success/failure message

### GeckoLib Animation

Animations use `RawAnimation.begin()` with chaining:
- `.thenLoop("animation_name")` - Loop animation
- `.thenPlay("animation_name")` - Play once
- `.thenPlayAndHold("animation_name")` - Play and hold last frame

### Packet Pattern

```java
public record ExamplePacket(String data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ExamplePacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "example"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExamplePacket> STREAM_CODEC = ...;

    public static void handle(ExamplePacket packet, IPayloadContext context) { ... }
}
```

## Dependencies

- **NeoForge** 21.1.215
- **GeckoLib** 4.8.2 - Animation library for entity models
- **Parchment Mappings** 2024.11.17 - Improved parameter names

## Testing

Run the client with:
```bash
./gradlew runClient
```

In-game testing:
1. Create NPC: `/talesmaker npc create ~ ~ ~ test_npc talesmaker:placeholder`
2. Test movement: `/talesmaker movement goto test_npc ~ ~ ~10`
3. Test dialogue: `/talesmaker dialogue say test_npc "Hello!"`
4. Remove NPC: `/talesmaker npc remove test_npc`

## Notes for AI Assistants

- The mod uses NeoForge event bus patterns (`@SubscribeEvent`, `@EventBusSubscriber`)
- Client-only code must be in `TalesMakerClient` or use `@OnlyIn(Dist.CLIENT)`
- Entity data accessors must be defined statically and registered in `defineSynchedData()`
- All resources (models, textures, animations) use `ResourceLocation` paths
- NPC presets are data-driven and loaded via `NpcPresetManager` as a reload listener
- Movement and look-at handlers are separate classes to keep `NpcEntity` manageable
- The dialogue system persists history per-world on the client side
