package dcs.jagermeistars.talesmaker.client.clue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dcs.jagermeistars.talesmaker.TalesMaker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses JSON item models to extract cube information for clue hotspots.
 * Cube names are used to match clues in presets.
 */
public class ClueModelParser {

    public enum RotationAxis {
        X,
        Y,
        Z
    }

    public record FaceData(
            float u1,
            float v1,
            float u2,
            float v2,
            int rotation
    ) {}

    public record RotationData(
            Vec3 origin,
            RotationAxis axis,
            float angle
    ) {}

    public record CubeData(
            String name,
            Vec3 from,
            Vec3 to,
            RotationData rotation,
            Map<Direction, FaceData> faces
    ) {}

    public record ModelData(
            List<CubeData> cubes,
            int textureWidth,
            int textureHeight
    ) {}

    public record HotspotData(
            String name,
            Vec3 center,
            float radius
    ) {}

    private record ParsedModel(
            ModelData model,
            Map<String, CubeData> cubesByName
    ) {}

    private static final int DEFAULT_TEX_SIZE = 16;
    private static final Vec3 MODEL_CENTER = new Vec3(8, 8, 8);

    private static final Map<ResourceLocation, ParsedModel> CACHE = new HashMap<>();

    public static void clearCache() {
        CACHE.clear();
    }

    public static ModelData getModelData(ResourceLocation modelPath) {
        ParsedModel parsed = getOrParseModel(modelPath);
        return parsed != null ? parsed.model() : null;
    }

    public static List<HotspotData> getHotspots(ResourceLocation modelPath, List<String> cubeNames) {
        ParsedModel parsed = getOrParseModel(modelPath);
        if (parsed == null) {
            return Collections.emptyList();
        }

        List<HotspotData> result = new ArrayList<>();
        for (String cubeName : cubeNames) {
            CubeData cube = parsed.cubesByName().get(cubeName);
            if (cube != null) {
                result.add(toHotspot(cube));
            } else {
                TalesMaker.LOGGER.warn("Cube '{}' not found in model {}", cubeName, modelPath);
            }
        }
        return result;
    }

    public static List<CubeData> getCubes(ResourceLocation modelPath, List<String> cubeNames) {
        ParsedModel parsed = getOrParseModel(modelPath);
        if (parsed == null) {
            return Collections.emptyList();
        }

        List<CubeData> result = new ArrayList<>();
        for (String cubeName : cubeNames) {
            CubeData cube = parsed.cubesByName().get(cubeName);
            if (cube != null) {
                result.add(cube);
            } else {
                TalesMaker.LOGGER.warn("Cube '{}' not found in model {}", cubeName, modelPath);
            }
        }
        return result;
    }

    private static HotspotData toHotspot(CubeData cube) {
        Vec3 from = cube.from();
        Vec3 to = cube.to();
        Vec3 center = from.add(to).scale(0.5);

        RotationData rotation = cube.rotation();
        if (rotation != null && rotation.angle() != 0) {
            center = rotate(center, rotation.origin(), rotation.axis(), rotation.angle());
        }

        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        float radius = (float) (Math.sqrt(dx * dx + dy * dy + dz * dz) / 2.0);

        return new HotspotData(cube.name(), center, radius);
    }

    private static ParsedModel getOrParseModel(ResourceLocation modelPath) {
        return CACHE.computeIfAbsent(modelPath, ClueModelParser::parseModel);
    }

    private static ParsedModel parseModel(ResourceLocation modelPath) {
        return parseModelInternal(modelPath, new HashSet<>());
    }

    private static ParsedModel parseModelInternal(ResourceLocation modelPath, Set<ResourceLocation> seen) {
        if (!seen.add(modelPath)) {
            TalesMaker.LOGGER.warn("Model parent loop detected for {}", modelPath);
            return null;
        }

        JsonObject root = readModelJson(modelPath);
        if (root == null) {
            return null;
        }

        ParsedModel parentModel = null;
        if (root.has("parent")) {
            ResourceLocation parentPath = resolveParent(modelPath, root.get("parent").getAsString());
            if (parentPath != null) {
                parentModel = parseModelInternal(parentPath, seen);
            }
        }

        int textureWidth = DEFAULT_TEX_SIZE;
        int textureHeight = DEFAULT_TEX_SIZE;
        if (root.has("texture_size")) {
            int[] size = parseTextureSize(root.getAsJsonArray("texture_size"));
            textureWidth = size[0];
            textureHeight = size[1];
        } else {
            int[] inferredSize = inferTextureSize(root);
            if (inferredSize != null) {
                textureWidth = inferredSize[0];
                textureHeight = inferredSize[1];
            } else if (parentModel != null) {
                textureWidth = parentModel.model().textureWidth();
                textureHeight = parentModel.model().textureHeight();
            }
        }

        float[] maxUv = getMaxUv(root);
        List<CubeData> cubes;
        if (root.has("elements")) {
            float scaleU = 1.0f;
            float scaleV = 1.0f;
            if (maxUv != null && textureWidth > DEFAULT_TEX_SIZE && textureHeight > DEFAULT_TEX_SIZE) {
                if (maxUv[0] <= DEFAULT_TEX_SIZE + 0.001f && maxUv[1] <= DEFAULT_TEX_SIZE + 0.001f) {
                    scaleU = (float) textureWidth / DEFAULT_TEX_SIZE;
                    scaleV = (float) textureHeight / DEFAULT_TEX_SIZE;
                }
            }
            cubes = parseElements(root.getAsJsonArray("elements"), textureWidth, textureHeight, scaleU, scaleV);
        } else if (parentModel != null) {
            cubes = parentModel.model().cubes();
        } else {
            cubes = List.of();
        }

        Map<String, CubeData> cubesByName = new HashMap<>();
        for (CubeData cube : cubes) {
            cubesByName.put(cube.name(), cube);
        }

        return new ParsedModel(new ModelData(cubes, textureWidth, textureHeight), cubesByName);
    }

    private static JsonObject readModelJson(ResourceLocation modelPath) {
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            var resource = resourceManager.getResource(modelPath);

            if (resource.isEmpty()) {
                TalesMaker.LOGGER.error("Model file not found: {}", modelPath);
                return null;
            }

            try (InputStream stream = resource.get().open();
                 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception e) {
            TalesMaker.LOGGER.error("Failed to parse model {}: {}", modelPath, e.getMessage());
            return null;
        }
    }

    private static ResourceLocation resolveParent(ResourceLocation modelPath, String parentId) {
        ResourceLocation parentLocation = ResourceLocation.tryParse(parentId);
        if (parentLocation == null) {
            TalesMaker.LOGGER.warn("Invalid parent id '{}' in {}", parentId, modelPath);
            return null;
        }

        String path = parentLocation.getPath();
        if (!path.startsWith("models/")) {
            path = "models/" + path;
        }
        if (!path.endsWith(".json")) {
            path = path + ".json";
        }

        try {
            return ResourceLocation.fromNamespaceAndPath(parentLocation.getNamespace(), path);
        } catch (Exception e) {
            TalesMaker.LOGGER.warn("Invalid parent path '{}' in {}", path, modelPath);
            return null;
        }
    }

    private static int[] parseTextureSize(JsonArray textureSizeArray) {
        if (textureSizeArray == null || textureSizeArray.size() < 2) {
            return new int[]{DEFAULT_TEX_SIZE, DEFAULT_TEX_SIZE};
        }
        int width = textureSizeArray.get(0).getAsInt();
        int height = textureSizeArray.get(1).getAsInt();
        return new int[]{Math.max(1, width), Math.max(1, height)};
    }

    private static int[] inferTextureSize(JsonObject root) {
        float[] maxUv = getMaxUv(root);
        if (maxUv == null) {
            return null;
        }

        int width = Math.max(DEFAULT_TEX_SIZE, (int) Math.ceil(maxUv[0]));
        int height = Math.max(DEFAULT_TEX_SIZE, (int) Math.ceil(maxUv[1]));
        return new int[]{width, height};
    }

    private static float[] getMaxUv(JsonObject root) {
        if (!root.has("elements")) {
            return null;
        }

        JsonArray elements = root.getAsJsonArray("elements");
        if (elements == null || elements.isEmpty()) {
            return null;
        }

        float maxU = 0.0f;
        float maxV = 0.0f;

        for (JsonElement element : elements) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject elementObj = element.getAsJsonObject();
            JsonObject facesObj = elementObj.getAsJsonObject("faces");
            if (facesObj == null) {
                continue;
            }

            for (Map.Entry<String, JsonElement> entry : facesObj.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }

                JsonObject faceObj = entry.getValue().getAsJsonObject();
                if (!faceObj.has("uv")) {
                    continue;
                }

                JsonArray uv = faceObj.getAsJsonArray("uv");
                if (uv == null || uv.size() < 4) {
                    continue;
                }

                maxU = Math.max(maxU, Math.max(uv.get(0).getAsFloat(), uv.get(2).getAsFloat()));
                maxV = Math.max(maxV, Math.max(uv.get(1).getAsFloat(), uv.get(3).getAsFloat()));
            }
        }

        if (maxU <= 0.0f && maxV <= 0.0f) {
            return null;
        }

        return new float[]{maxU, maxV};
    }

    private static List<CubeData> parseElements(JsonArray elementsArray, int textureWidth, int textureHeight,
                                                float scaleU, float scaleV) {
        if (elementsArray == null || elementsArray.isEmpty()) {
            return List.of();
        }

        List<CubeData> cubes = new ArrayList<>();
        int index = 0;

        for (JsonElement element : elementsArray) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject elementObj = element.getAsJsonObject();
            String name = elementObj.has("name") ? elementObj.get("name").getAsString() : "cube_" + index;
            index++;

            Vec3 from = parseVec3(elementObj, "from").subtract(MODEL_CENTER);
            Vec3 to = parseVec3(elementObj, "to").subtract(MODEL_CENTER);

            RotationData rotation = null;
            if (elementObj.has("rotation")) {
                rotation = parseRotation(elementObj.getAsJsonObject("rotation"));
            }

            Map<Direction, FaceData> faces = parseFaces(elementObj.getAsJsonObject("faces"), textureWidth, textureHeight,
                    scaleU, scaleV);

            cubes.add(new CubeData(name, from, to, rotation, faces));
        }

        return cubes;
    }

    private static Vec3 parseVec3(JsonObject obj, String key) {
        if (!obj.has(key)) {
            return Vec3.ZERO;
        }
        JsonArray array = obj.getAsJsonArray(key);
        if (array == null || array.size() < 3) {
            return Vec3.ZERO;
        }
        return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }

    private static RotationData parseRotation(JsonObject rotationObj) {
        if (rotationObj == null) {
            return null;
        }

        Vec3 origin = MODEL_CENTER;
        if (rotationObj.has("origin")) {
            JsonArray originArray = rotationObj.getAsJsonArray("origin");
            if (originArray != null && originArray.size() >= 3) {
                origin = new Vec3(
                        originArray.get(0).getAsDouble(),
                        originArray.get(1).getAsDouble(),
                        originArray.get(2).getAsDouble()
                ).subtract(MODEL_CENTER);
            }
        }

        RotationAxis axis = RotationAxis.Y;
        if (rotationObj.has("axis")) {
            String axisStr = rotationObj.get("axis").getAsString();
            if ("x".equalsIgnoreCase(axisStr)) {
                axis = RotationAxis.X;
            } else if ("z".equalsIgnoreCase(axisStr)) {
                axis = RotationAxis.Z;
            }
        }

        float angle = rotationObj.has("angle") ? rotationObj.get("angle").getAsFloat() : 0.0f;

        return new RotationData(origin, axis, angle);
    }

    private static Map<Direction, FaceData> parseFaces(JsonObject facesObj, int textureWidth, int textureHeight,
                                                      float scaleU, float scaleV) {
        if (facesObj == null) {
            return Collections.emptyMap();
        }

        Map<Direction, FaceData> faces = new EnumMap<>(Direction.class);
        for (Map.Entry<String, JsonElement> entry : facesObj.entrySet()) {
            Direction direction = Direction.byName(entry.getKey());
            if (direction == null || !entry.getValue().isJsonObject()) {
                continue;
            }

            JsonObject faceObj = entry.getValue().getAsJsonObject();
            FaceData faceData = parseFace(faceObj, textureWidth, textureHeight, scaleU, scaleV);
            faces.put(direction, faceData);
        }

        return faces;
    }

    private static FaceData parseFace(JsonObject faceObj, int textureWidth, int textureHeight,
                                      float scaleU, float scaleV) {
        float u1 = 0.0f;
        float v1 = 0.0f;
        float u2 = 1.0f;
        float v2 = 1.0f;

        if (faceObj.has("uv")) {
            JsonArray uv = faceObj.getAsJsonArray("uv");
            if (uv != null && uv.size() >= 4) {
                u1 = (uv.get(0).getAsFloat() * scaleU) / textureWidth;
                v1 = (uv.get(1).getAsFloat() * scaleV) / textureHeight;
                u2 = (uv.get(2).getAsFloat() * scaleU) / textureWidth;
                v2 = (uv.get(3).getAsFloat() * scaleV) / textureHeight;
            }
        }

        int rotation = faceObj.has("rotation") ? faceObj.get("rotation").getAsInt() : 0;
        return new FaceData(u1, v1, u2, v2, rotation);
    }

    private static Vec3 rotate(Vec3 point, Vec3 origin, RotationAxis axis, float angleDeg) {
        double angle = Math.toRadians(angleDeg);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        Vec3 translated = point.subtract(origin);
        double x = translated.x;
        double y = translated.y;
        double z = translated.z;

        double rx = x;
        double ry = y;
        double rz = z;

        switch (axis) {
            case X -> {
                ry = y * cos - z * sin;
                rz = y * sin + z * cos;
            }
            case Y -> {
                rx = x * cos + z * sin;
                rz = -x * sin + z * cos;
            }
            case Z -> {
                rx = x * cos - y * sin;
                ry = x * sin + y * cos;
            }
        }

        return new Vec3(rx, ry, rz).add(origin);
    }
}
