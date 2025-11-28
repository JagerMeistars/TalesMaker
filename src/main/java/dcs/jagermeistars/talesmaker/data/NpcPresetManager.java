package dcs.jagermeistars.talesmaker.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dcs.jagermeistars.talesmaker.TalesMaker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class NpcPresetManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "npc/presets";

    private final Map<ResourceLocation, NpcPreset> presets = new HashMap<>();

    public NpcPresetManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        presets.clear();

        objects.forEach((fileId, json) -> {
            try {
                // Check if JSON is an array (multiple presets) or object (single preset)
                if (json.isJsonArray()) {
                    // Parse array of presets
                    JsonArray array = json.getAsJsonArray();
                    for (JsonElement element : array) {
                        parsePreset(fileId.getNamespace(), element);
                    }
                } else {
                    // Parse single preset (backward compatibility)
                    parsePreset(fileId.getNamespace(), json);
                }
            } catch (Exception e) {
                TalesMaker.LOGGER.error("Error loading NPC preset file {}", fileId, e);
            }
        });

        TalesMaker.LOGGER.info("Loaded {} NPC presets", presets.size());
    }

    private void parsePreset(String namespace, JsonElement json) {
        try {
            NpcPreset.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> TalesMaker.LOGGER.error("Failed to parse NPC preset: {}", error))
                    .ifPresent(preset -> {
                        // Extract simple ID from JSON
                        String simpleId = json.getAsJsonObject().get("id").getAsString();

                        // Construct full ResourceLocation: namespace:id
                        ResourceLocation fullId = ResourceLocation.fromNamespaceAndPath(namespace, simpleId);

                        // Create preset with full ID
                        NpcPreset fullPreset = preset.withId(fullId);

                        presets.put(fullId, fullPreset);
                        TalesMaker.LOGGER.info("Loaded NPC preset: {}", fullId);
                    });
        } catch (Exception e) {
            TalesMaker.LOGGER.error("Error parsing NPC preset", e);
        }
    }

    public Optional<NpcPreset> getPreset(ResourceLocation id) {
        return Optional.ofNullable(presets.get(id));
    }

    public Map<ResourceLocation, NpcPreset> getAllPresets() {
        return Map.copyOf(presets);
    }
}
