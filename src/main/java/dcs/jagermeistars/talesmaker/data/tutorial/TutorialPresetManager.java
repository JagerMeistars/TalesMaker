package dcs.jagermeistars.talesmaker.data.tutorial;

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
 * Manages loading and storage of tutorial presets from datapacks.
 *
 * Presets are loaded from: data/<namespace>/tutorials/<id>.json
 */
public class TutorialPresetManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "tutorials";

    private final Map<ResourceLocation, TutorialPreset> presets = new HashMap<>();
    private final List<String> loadErrors = new ArrayList<>();

    public TutorialPresetManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        presets.clear();
        loadErrors.clear();

        objects.forEach((fileId, json) -> {
            try {
                parsePreset(fileId, json);
            } catch (Exception e) {
                loadErrors.add("Tutorial file " + fileId + ": " + e.getMessage());
                TalesMaker.LOGGER.error("Failed to load tutorial preset {}: {}", fileId, e.getMessage());
            }
        });

        TalesMaker.LOGGER.info("Loaded {} tutorial presets", presets.size());

        for (String error : loadErrors) {
            ModNetworking.sendWarningToAll(error);
        }
    }

    private void parsePreset(ResourceLocation fileId, JsonElement json) {
        final String[] errorHolder = {null};

        TutorialPreset.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> errorHolder[0] = error)
                .ifPresent(preset -> presets.put(fileId, preset.withId(fileId)));

        if (errorHolder[0] != null) {
            loadErrors.add(fileId + ": " + errorHolder[0]);
        }
    }

    public Optional<TutorialPreset> getPreset(ResourceLocation id) {
        return Optional.ofNullable(presets.get(id));
    }

    public Map<ResourceLocation, TutorialPreset> getAllPresets() {
        return Map.copyOf(presets);
    }
}
