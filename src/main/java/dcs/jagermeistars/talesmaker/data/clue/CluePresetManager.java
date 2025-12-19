package dcs.jagermeistars.talesmaker.data.clue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.network.ModNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages loading and storage of clue inspection presets from datapacks.
 *
 * Clue presets are loaded from: data/<namespace>/clues/<id>.json
 */
public class CluePresetManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "clues";

    private final Map<ResourceLocation, CluePreset> presets = new HashMap<>();
    private final List<String> loadErrors = new ArrayList<>();

    public CluePresetManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        presets.clear();
        loadErrors.clear();

        objects.forEach((fileId, json) -> {
            try {
                parseCluePreset(fileId, json);
            } catch (Exception e) {
                loadErrors.add("Clue file " + fileId + ": " + e.getMessage());
                TalesMaker.LOGGER.error("Failed to load clue preset {}: {}", fileId, e.getMessage());
            }
        });

        TalesMaker.LOGGER.info("Loaded {} clue presets", presets.size());

        // Send errors as warnings to players
        for (String error : loadErrors) {
            ModNetworking.sendWarningToAll(error);
        }
    }

    private void parseCluePreset(ResourceLocation fileId, JsonElement json) {
        final String[] errorHolder = {null};

        CluePreset.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> errorHolder[0] = error)
                .ifPresent(preset -> {
                    // Build full ID from file path
                    CluePreset fullPreset = preset.withId(fileId);
                    presets.put(fileId, fullPreset);
                });

        if (errorHolder[0] != null) {
            loadErrors.add(fileId + ": " + errorHolder[0]);
        }
    }

    /**
     * Get a clue preset by its ID.
     *
     * @param id The resource location of the clue preset
     * @return Optional containing the clue preset if found
     */
    public Optional<CluePreset> getPreset(ResourceLocation id) {
        return Optional.ofNullable(presets.get(id));
    }

    /**
     * Get all loaded clue presets.
     *
     * @return Unmodifiable map of all clue presets
     */
    public Map<ResourceLocation, CluePreset> getAllPresets() {
        return Map.copyOf(presets);
    }

    /**
     * Check if a clue preset exists.
     *
     * @param id The resource location to check
     * @return true if the preset exists
     */
    public boolean hasPreset(ResourceLocation id) {
        return presets.containsKey(id);
    }
}
