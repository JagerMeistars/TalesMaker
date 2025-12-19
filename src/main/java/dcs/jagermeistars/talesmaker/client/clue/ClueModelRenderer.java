package dcs.jagermeistars.talesmaker.client.clue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtil;

import java.util.List;
import java.util.Optional;

/**
 * Renderer for clue models in the inspection screen.
 * Provides 3D model rendering with rotation/zoom and ray-picking for hotspot detection.
 */
public class ClueModelRenderer {
    private final ClueGeoModel model;

    // Rendering context
    private float rotationX = 0;
    private float rotationY = 0;
    private float zoom = 1.0f;

    // Screen dimensions for ray-picking
    private int screenWidth;
    private int screenHeight;
    private int modelCenterX;
    private int modelCenterY;
    private float modelScale;

    public ClueModelRenderer() {
        this.model = new ClueGeoModel();
    }

    /**
     * Set rotation angles for the model (in degrees).
     */
    public void setRotation(float rotX, float rotY) {
        this.rotationX = rotX;
        this.rotationY = rotY;
    }

    /**
     * Set zoom level (1.0 = default).
     */
    public void setZoom(float zoom) {
        this.zoom = Math.max(0.5f, Math.min(3.0f, zoom));
    }

    public float getZoom() {
        return zoom;
    }

    /**
     * Set screen parameters for ray-picking calculations.
     */
    public void setScreenParams(int screenWidth, int screenHeight, int modelCenterX, int modelCenterY, float modelScale) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.modelCenterX = modelCenterX;
        this.modelCenterY = modelCenterY;
        this.modelScale = modelScale;
    }

    /**
     * Render the clue model.
     */
    public void render(ClueRenderEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        ResourceLocation modelPath = entity.getModelPath();
        ResourceLocation texturePath = entity.getTexturePath();

        if (modelPath == null || texturePath == null) {
            return;
        }

        // Get baked model from cache
        BakedGeoModel bakedModel = GeckoLibCache.getBakedModels().get(modelPath);
        if (bakedModel == null) {
            return;
        }

        poseStack.pushPose();

        // Apply zoom
        float scale = modelScale * zoom;
        poseStack.scale(scale, -scale, scale); // Flip Y for GUI

        // Apply rotation
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rotationX));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotationY));

        // Get render type and buffer
        RenderType renderType = RenderType.entityCutoutNoCull(texturePath);
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        // Render bones recursively
        for (GeoBone bone : bakedModel.topLevelBones()) {
            renderBone(poseStack, bone, buffer, packedLight, 0xF000F0, 1, 1, 1, 1);
        }

        poseStack.popPose();
    }

    /**
     * Recursively render a bone and its children.
     */
    private void renderBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                            int packedLight, int packedOverlay, float r, float g, float b, float a) {
        poseStack.pushPose();

        // Apply bone transformations
        RenderUtil.translateToPivotPoint(poseStack, bone);
        RenderUtil.rotateMatrixAroundBone(poseStack, bone);
        RenderUtil.scaleMatrixForBone(poseStack, bone);
        RenderUtil.translateAwayFromPivotPoint(poseStack, bone);

        // Render cubes
        if (!bone.isHidden()) {
            for (GeoCube cube : bone.getCubes()) {
                renderCube(poseStack, cube, buffer, packedLight, packedOverlay, r, g, b, a);
            }
        }

        // Render children
        for (GeoBone child : bone.getChildBones()) {
            renderBone(poseStack, child, buffer, packedLight, packedOverlay, r, g, b, a);
        }

        poseStack.popPose();
    }

    /**
     * Render a cube.
     */
    private void renderCube(PoseStack poseStack, GeoCube cube, VertexConsumer buffer,
                            int packedLight, int packedOverlay, float r, float g, float b, float a) {
        var pose = poseStack.last();

        for (GeoQuad quad : cube.quads()) {
            if (quad == null) continue;

            Vector3f normal = new Vector3f(quad.normal());
            normal.mul(pose.normal());

            for (GeoVertex vertex : quad.vertices()) {
                Vector3f pos = new Vector3f(vertex.position());
                pos.mulPosition(pose.pose());

                buffer.addVertex(pos.x, pos.y, pos.z)
                        .setColor(r, g, b, a)
                        .setUv(vertex.texU(), vertex.texV())
                        .setOverlay(packedOverlay)
                        .setLight(packedLight)
                        .setNormal(normal.x, normal.y, normal.z);
            }
        }
    }

    /**
     * Check if mouse position hits any hotspot and return the bone name.
     */
    public Optional<String> pickHotspot(double mouseX, double mouseY, List<ClueModelParser.BoneData> hotspots) {
        if (hotspots.isEmpty()) {
            return Optional.empty();
        }

        // Convert screen coordinates to normalized device coordinates relative to model center
        float ndcX = (float) ((mouseX - modelCenterX) / (modelScale * zoom * 16));
        float ndcY = (float) ((modelCenterY - mouseY) / (modelScale * zoom * 16)); // Y is inverted

        // Create ray origin (camera position - looking from front)
        Vector3f rayOrigin = new Vector3f(0, 0, 10);

        // Ray direction (towards the center, adjusted by mouse offset)
        Vector3f rayDir = new Vector3f(ndcX, ndcY, -1).normalize();

        // Apply inverse rotation to ray direction
        float rotXRad = (float) Math.toRadians(-rotationX);
        float rotYRad = (float) Math.toRadians(-rotationY);

        // Rotate around Y axis first, then X
        rotateVectorY(rayDir, rotYRad);
        rotateVectorX(rayDir, rotXRad);
        rayDir.normalize();

        // Also rotate ray origin
        rotateVectorY(rayOrigin, rotYRad);
        rotateVectorX(rayOrigin, rotXRad);

        // Find closest hotspot intersection
        String closestBone = null;
        float minDist = Float.MAX_VALUE;

        for (ClueModelParser.BoneData hotspot : hotspots) {
            Vec3 pos = hotspot.position();
            Vector3f sphereCenter = new Vector3f((float) pos.x, (float) pos.y, (float) pos.z);
            float radius = hotspot.radius();

            float dist = raySphereIntersect(rayOrigin, rayDir, sphereCenter, radius);
            if (dist >= 0 && dist < minDist) {
                minDist = dist;
                closestBone = hotspot.name();
            }
        }

        return Optional.ofNullable(closestBone);
    }

    /**
     * Rotate a vector around the X axis.
     */
    private void rotateVectorX(Vector3f v, float angleRad) {
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);
        float y = v.y * cos - v.z * sin;
        float z = v.y * sin + v.z * cos;
        v.y = y;
        v.z = z;
    }

    /**
     * Rotate a vector around the Y axis.
     */
    private void rotateVectorY(Vector3f v, float angleRad) {
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);
        float x = v.x * cos + v.z * sin;
        float z = -v.x * sin + v.z * cos;
        v.x = x;
        v.z = z;
    }

    /**
     * Ray-sphere intersection test.
     */
    private float raySphereIntersect(Vector3f rayOrigin, Vector3f rayDir, Vector3f sphereCenter, float radius) {
        Vector3f oc = new Vector3f(rayOrigin).sub(sphereCenter);

        float a = rayDir.dot(rayDir);
        float b = 2.0f * oc.dot(rayDir);
        float c = oc.dot(oc) - radius * radius;

        float discriminant = b * b - 4 * a * c;

        if (discriminant < 0) {
            return -1;
        }

        float sqrtDisc = (float) Math.sqrt(discriminant);
        float t1 = (-b - sqrtDisc) / (2 * a);
        float t2 = (-b + sqrtDisc) / (2 * a);

        if (t1 > 0) return t1;
        if (t2 > 0) return t2;
        return -1;
    }
}
