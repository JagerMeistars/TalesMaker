package dcs.jagermeistars.talesmaker.data.choice;

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
 * Manages loading and storage of choice window definitions from datapacks.
 *
 * Choice windows are loaded from: data/<namespace>/choices/<id>.json
 */
public class ChoiceWindowManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "choices";

    private final Map<ResourceLocation, ChoiceWindow> windows = new HashMap<>();
    private final List<String> loadErrors = new ArrayList<>();

    public ChoiceWindowManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        windows.clear();
        loadErrors.clear();

        objects.forEach((fileId, json) -> {
            try {
                parseChoiceWindow(fileId, json);
            } catch (Exception e) {
                loadErrors.add("Choice file " + fileId + ": " + e.getMessage());
                TalesMaker.LOGGER.error("Failed to load choice window {}: {}", fileId, e.getMessage());
            }
        });

        TalesMaker.LOGGER.info("Loaded {} choice windows", windows.size());

        // Send errors as warnings to players
        for (String error : loadErrors) {
            ModNetworking.sendWarningToAll(error);
        }
    }

    private void parseChoiceWindow(ResourceLocation fileId, JsonElement json) {
        final String[] errorHolder = {null};

        ChoiceWindow.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> errorHolder[0] = error)
                .ifPresent(window -> {
                    // Build full ID from file path
                    // fileId is like "namespace:path/to/file" from the JSON location
                    ChoiceWindow fullWindow = window.withId(fileId);
                    windows.put(fileId, fullWindow);
                });

        if (errorHolder[0] != null) {
            loadErrors.add(fileId + ": " + errorHolder[0]);
        }
    }

    /**
     * Get a choice window by its ID.
     *
     * @param id The resource location of the choice window
     * @return Optional containing the choice window if found
     */
    public Optional<ChoiceWindow> getWindow(ResourceLocation id) {
        return Optional.ofNullable(windows.get(id));
    }

    /**
     * Get all loaded choice windows.
     *
     * @return Unmodifiable map of all choice windows
     */
    public Map<ResourceLocation, ChoiceWindow> getAllWindows() {
        return Map.copyOf(windows);
    }

    /**
     * Check if a choice window exists.
     *
     * @param id The resource location to check
     * @return true if the window exists
     */
    public boolean hasWindow(ResourceLocation id) {
        return windows.containsKey(id);
    }
}
