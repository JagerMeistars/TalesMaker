package dcs.jagermeistars.talesmaker.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dcs.jagermeistars.talesmaker.entity.NpcEntity;
import dcs.jagermeistars.talesmaker.network.ModNetworking;
import dcs.jagermeistars.talesmaker.network.ReloadNotifyPacket;
import dcs.jagermeistars.talesmaker.network.ValidateResourcesPacket;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NpcPresetManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "npc/presets";

    private final Map<ResourceLocation, NpcPreset> presets = new HashMap<>();
    private final List<String> loadErrors = new ArrayList<>();

    public NpcPresetManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        presets.clear();
        loadErrors.clear();

        objects.forEach((fileId, json) -> {
            try {
                if (json.isJsonArray()) {
                    JsonArray array = json.getAsJsonArray();
                    for (JsonElement element : array) {
                        parsePreset(fileId.getNamespace(), element);
                    }
                } else {
                    parsePreset(fileId.getNamespace(), json);
                }
            } catch (Exception e) {
                loadErrors.add("File " + fileId + ": " + e.getMessage());
            }
        });

        // Update all existing NPCs and send notifications
        updateAllNpcsAndNotify();
    }

    private void updateAllNpcsAndNotify() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerLevel level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof NpcEntity npc) {
                    ResourceLocation presetId = npc.getPresetResourceLocation();
                    if (presetId != null) {
                        NpcPreset preset = presets.get(presetId);
                        if (preset != null) {
                            npc.setPreset(preset);
                        }
                    }
                }
            }
        }

        // Send preset/resource errors as warnings (yellow)
        for (String error : loadErrors) {
            ModNetworking.sendWarningToAll(error);
        }

        // Send reload notification to trigger client-side NPC validation
        PacketDistributor.sendToAllPlayers(new ReloadNotifyPacket(presets.size(), !loadErrors.isEmpty()));

        // Request resource validation from all clients for each preset
        for (Map.Entry<ResourceLocation, NpcPreset> entry : presets.entrySet()) {
            NpcPreset preset = entry.getValue();
            PacketDistributor.sendToAllPlayers(new ValidateResourcesPacket(
                    entry.getKey().toString(),
                    preset.model().toString(),
                    preset.texture().toString(),
                    preset.emissive() != null ? preset.emissive().toString() : "",
                    preset.animations().path().toString()
            ));
        }
    }

    private void parsePreset(String namespace, JsonElement json) {
        final String[] errorHolder = {null};

        NpcPreset.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> errorHolder[0] = error)
                .ifPresent(preset -> {
                    try {
                        String simpleId = json.getAsJsonObject().get("id").getAsString();
                        ResourceLocation fullId = ResourceLocation.fromNamespaceAndPath(namespace, simpleId);
                        NpcPreset fullPreset = preset.withId(fullId);
                        presets.put(fullId, fullPreset);
                    } catch (Exception e) {
                        loadErrors.add(namespace + ": Missing or invalid 'id' field");
                    }
                });

        if (errorHolder[0] != null) {
            loadErrors.add(namespace + ": " + errorHolder[0]);
        }
    }

    public Optional<NpcPreset> getPreset(ResourceLocation id) {
        return Optional.ofNullable(presets.get(id));
    }

    public Map<ResourceLocation, NpcPreset> getAllPresets() {
        return Map.copyOf(presets);
    }
}
