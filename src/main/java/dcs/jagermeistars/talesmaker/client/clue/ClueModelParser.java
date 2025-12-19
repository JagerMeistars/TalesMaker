package dcs.jagermeistars.talesmaker.client.clue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dcs.jagermeistars.talesmaker.TalesMaker;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parses GeckoLib .geo.json model files to extract bone information for clue hotspots.
 */
public class ClueModelParser {

    /**
     * Data about a bone in the model.
     */
    public record BoneData(
            String name,
            Vec3 position,  // World position (accumulated from parent hierarchy)
            float radius    // Calculated from cube size
    ) {}

    /**
     * Internal representation of a parsed bone.
     */
    private record ParsedBone(
            String name,
            String parent,
            Vec3 pivot,
            float radius
    ) {}

    /**
     * Cached parsed model data.
     */
    private record ParsedModel(
            Map<String, ParsedBone> bones
    ) {}

    // Cache for parsed models
    private static final Map<ResourceLocation, ParsedModel> cache = new HashMap<>();

    /**
     * Clear the model cache. Should be called on resource reload.
     */
    public static void clearCache() {
        cache.clear();
    }

    /**
     * Get data for a specific bone by name.
     *
     * @param modelPath Path to the .geo.json model file
     * @param boneName  Name of the bone to find
     * @return Optional containing bone data if found
     */
    public static Optional<BoneData> getBoneData(ResourceLocation modelPath, String boneName) {
        ParsedModel model = getOrParseModel(modelPath);
        if (model == null) {
            return Optional.empty();
        }

        ParsedBone bone = model.bones().get(boneName);
        if (bone == null) {
            TalesMaker.LOGGER.warn("Bone '{}' not found in model {}", boneName, modelPath);
            return Optional.empty();
        }

        // Calculate world position by accumulating parent pivots
        Vec3 worldPos = calculateWorldPosition(model, bone);

        return Optional.of(new BoneData(bone.name(), worldPos, bone.radius()));
    }

    /**
     * Get data for multiple bones by name.
     *
     * @param modelPath Path to the .geo.json model file
     * @param boneNames List of bone names to find
     * @return List of bone data (only for bones that were found)
     */
    public static List<BoneData> getBonesData(ResourceLocation modelPath, List<String> boneNames) {
        ParsedModel model = getOrParseModel(modelPath);
        if (model == null) {
            return Collections.emptyList();
        }

        List<BoneData> result = new ArrayList<>();
        for (String boneName : boneNames) {
            ParsedBone bone = model.bones().get(boneName);
            if (bone != null) {
                Vec3 worldPos = calculateWorldPosition(model, bone);
                result.add(new BoneData(bone.name(), worldPos, bone.radius()));
            } else {
                TalesMaker.LOGGER.warn("Bone '{}' not found in model {}", boneName, modelPath);
            }
        }
        return result;
    }

    /**
     * Calculate world position by accumulating parent pivots.
     */
    private static Vec3 calculateWorldPosition(ParsedModel model, ParsedBone bone) {
        Vec3 worldPos = bone.pivot();
        String parentName = bone.parent();

        while (parentName != null && !parentName.isEmpty()) {
            ParsedBone parent = model.bones().get(parentName);
            if (parent == null) {
                break;
            }
            worldPos = worldPos.add(parent.pivot());
            parentName = parent.parent();
        }

        return worldPos;
    }

    /**
     * Get or parse model from cache.
     */
    private static ParsedModel getOrParseModel(ResourceLocation modelPath) {
        return cache.computeIfAbsent(modelPath, ClueModelParser::parseModel);
    }

    /**
     * Parse a .geo.json model file.
     */
    private static ParsedModel parseModel(ResourceLocation modelPath) {
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            var resource = resourceManager.getResource(modelPath);

            if (resource.isEmpty()) {
                TalesMaker.LOGGER.error("Model file not found: {}", modelPath);
                return null;
            }

            try (InputStream stream = resource.get().open();
                 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {

                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                return parseModelJson(root);
            }
        } catch (Exception e) {
            TalesMaker.LOGGER.error("Failed to parse model {}: {}", modelPath, e.getMessage());
            return null;
        }
    }

    /**
     * Parse the JSON structure of a .geo.json model.
     */
    private static ParsedModel parseModelJson(JsonObject root) {
        Map<String, ParsedBone> bones = new HashMap<>();

        // Get minecraft:geometry array
        JsonArray geometryArray = root.getAsJsonArray("minecraft:geometry");
        if (geometryArray == null || geometryArray.isEmpty()) {
            return new ParsedModel(bones);
        }

        // Get first geometry (usually only one)
        JsonObject geometry = geometryArray.get(0).getAsJsonObject();

        // Get bones array
        JsonArray bonesArray = geometry.getAsJsonArray("bones");
        if (bonesArray == null) {
            return new ParsedModel(bones);
        }

        // Parse each bone
        for (JsonElement boneElement : bonesArray) {
            JsonObject boneObj = boneElement.getAsJsonObject();
            ParsedBone bone = parseBone(boneObj);
            bones.put(bone.name(), bone);
        }

        return new ParsedModel(bones);
    }

    /**
     * Parse a single bone from JSON.
     */
    private static ParsedBone parseBone(JsonObject boneObj) {
        String name = boneObj.get("name").getAsString();

        // Get parent (optional)
        String parent = null;
        if (boneObj.has("parent")) {
            parent = boneObj.get("parent").getAsString();
        }

        // Get pivot (default to 0,0,0)
        Vec3 pivot = Vec3.ZERO;
        if (boneObj.has("pivot")) {
            JsonArray pivotArray = boneObj.getAsJsonArray("pivot");
            pivot = new Vec3(
                    pivotArray.get(0).getAsDouble(),
                    pivotArray.get(1).getAsDouble(),
                    pivotArray.get(2).getAsDouble()
            );
        }

        // Calculate radius from cubes (if any)
        float radius = calculateRadiusFromCubes(boneObj);

        return new ParsedBone(name, parent, pivot, radius);
    }

    /**
     * Calculate the radius for a bone based on its cube sizes.
     * Uses the largest dimension of the first cube divided by 2.
     * If no cubes, returns a default radius.
     */
    private static float calculateRadiusFromCubes(JsonObject boneObj) {
        if (!boneObj.has("cubes")) {
            return 1.0f; // Default radius for bones without geometry
        }

        JsonArray cubesArray = boneObj.getAsJsonArray("cubes");
        if (cubesArray.isEmpty()) {
            return 1.0f;
        }

        // Get first cube's size
        JsonObject firstCube = cubesArray.get(0).getAsJsonObject();
        if (!firstCube.has("size")) {
            return 1.0f;
        }

        JsonArray sizeArray = firstCube.getAsJsonArray("size");
        float width = sizeArray.get(0).getAsFloat();
        float height = sizeArray.get(1).getAsFloat();
        float depth = sizeArray.get(2).getAsFloat();

        // Use the largest dimension as diameter, divide by 2 for radius
        float maxSize = Math.max(width, Math.max(height, depth));
        return maxSize / 2.0f;
    }
}
