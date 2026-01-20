package dcs.jagermeistars.talesmaker.client.clue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Renderer for clue models in the inspection screen.
 * Provides 3D model rendering with rotation/zoom and ray-picking for hotspot detection.
 */
public class ClueModelRenderer {

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

    public void setRotation(float rotX, float rotY) {
        this.rotationX = rotX;
        this.rotationY = rotY;
    }

    public void setZoom(float zoom) {
        this.zoom = Math.max(0.5f, Math.min(3.0f, zoom));
    }

    public float getZoom() {
        return zoom;
    }

    public void setScreenParams(int screenWidth, int screenHeight, int modelCenterX, int modelCenterY, float modelScale) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.modelCenterX = modelCenterX;
        this.modelCenterY = modelCenterY;
        this.modelScale = modelScale;
    }

    public void render(ClueModelParser.ModelData modelData, ResourceLocation texturePath,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (modelData == null || texturePath == null) {
            return;
        }

        List<ClueModelParser.CubeData> cubes = modelData.cubes();
        if (cubes.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        float scale = modelScale * zoom;
        poseStack.scale(scale, -scale, scale);

        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rotationX));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotationY));

        RenderType renderType = RenderType.entityCutoutNoCull(normalizeTexturePath(texturePath));
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        for (ClueModelParser.CubeData cube : cubes) {
            renderCube(poseStack, cube, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        }

        poseStack.popPose();
    }

    private ResourceLocation normalizeTexturePath(ResourceLocation texturePath) {
        String path = texturePath.getPath();
        if (path.startsWith("textures/") && path.endsWith(".png")) {
            return texturePath;
        }

        String normalized = path;
        if (!normalized.startsWith("textures/")) {
            normalized = "textures/" + normalized;
        }
        if (!normalized.endsWith(".png")) {
            normalized = normalized + ".png";
        }

        try {
            return ResourceLocation.fromNamespaceAndPath(texturePath.getNamespace(), normalized);
        } catch (Exception e) {
            return texturePath;
        }
    }

    private void renderCube(PoseStack poseStack, ClueModelParser.CubeData cube, VertexConsumer buffer,
                            int packedLight, int packedOverlay) {
        poseStack.pushPose();

        applyRotation(poseStack, cube.rotation());

        Vec3 from = cube.from();
        Vec3 to = cube.to();

        float x1 = (float) Math.min(from.x, to.x);
        float y1 = (float) Math.min(from.y, to.y);
        float z1 = (float) Math.min(from.z, to.z);
        float x2 = (float) Math.max(from.x, to.x);
        float y2 = (float) Math.max(from.y, to.y);
        float z2 = (float) Math.max(from.z, to.z);

        PoseStack.Pose pose = poseStack.last();

        addFace(pose, buffer, x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1,
                Direction.NORTH, cube, packedLight, packedOverlay);
        addFace(pose, buffer, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2,
                Direction.SOUTH, cube, packedLight, packedOverlay);
        addFace(pose, buffer, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1,
                Direction.WEST, cube, packedLight, packedOverlay);
        addFace(pose, buffer, x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2,
                Direction.EAST, cube, packedLight, packedOverlay);
        addFace(pose, buffer, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1,
                Direction.UP, cube, packedLight, packedOverlay);
        addFace(pose, buffer, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2,
                Direction.DOWN, cube, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private void applyRotation(PoseStack poseStack, ClueModelParser.RotationData rotation) {
        if (rotation == null || rotation.angle() == 0) {
            return;
        }

        Vec3 origin = rotation.origin();
        poseStack.translate(origin.x, origin.y, origin.z);

        switch (rotation.axis()) {
            case X -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rotation.angle()));
            case Y -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation.angle()));
            case Z -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation.angle()));
        }

        poseStack.translate(-origin.x, -origin.y, -origin.z);
    }

    private void addFace(PoseStack.Pose pose, VertexConsumer buffer,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,
                         Direction direction, ClueModelParser.CubeData cube,
                         int packedLight, int packedOverlay) {
        ClueModelParser.FaceData face = cube.faces().get(direction);
        if (face == null) {
            face = new ClueModelParser.FaceData(0.0f, 0.0f, 1.0f, 1.0f, 0);
        }

        Vec3 from = cube.from();
        Vec3 to = cube.to();

        float xMin = (float) Math.min(from.x, to.x);
        float xMax = (float) Math.max(from.x, to.x);
        float yMin = (float) Math.min(from.y, to.y);
        float yMax = (float) Math.max(from.y, to.y);
        float zMin = (float) Math.min(from.z, to.z);
        float zMax = (float) Math.max(from.z, to.z);

        Vector3f normal = new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        normal.mul(pose.normal());

        float[] uv1 = computeUV(direction, face, x1, y1, z1, xMin, xMax, yMin, yMax, zMin, zMax);
        float[] uv2 = computeUV(direction, face, x2, y2, z2, xMin, xMax, yMin, yMax, zMin, zMax);
        float[] uv3 = computeUV(direction, face, x3, y3, z3, xMin, xMax, yMin, yMax, zMin, zMax);
        float[] uv4 = computeUV(direction, face, x4, y4, z4, xMin, xMax, yMin, yMax, zMin, zMax);

        addVertex(buffer, pose, x1, y1, z1, uv1[0], uv1[1], normal, packedLight, packedOverlay);
        addVertex(buffer, pose, x2, y2, z2, uv2[0], uv2[1], normal, packedLight, packedOverlay);
        addVertex(buffer, pose, x3, y3, z3, uv3[0], uv3[1], normal, packedLight, packedOverlay);
        addVertex(buffer, pose, x4, y4, z4, uv4[0], uv4[1], normal, packedLight, packedOverlay);
    }

    private float[] computeUV(Direction direction, ClueModelParser.FaceData face,
                              float x, float y, float z,
                              float xMin, float xMax,
                              float yMin, float yMax,
                              float zMin, float zMax) {
        float uMin = face.u1();
        float uMax = face.u2();
        float vMin = face.v1();
        float vMax = face.v2();

        float uT = 0.0f;
        float vT = 0.0f;

        float xSize = xMax - xMin;
        float ySize = yMax - yMin;
        float zSize = zMax - zMin;

        switch (direction) {
            case NORTH -> {
                uT = safeRatio(xMax - x, xSize);
                vT = safeRatio(y - yMin, ySize);
            }
            case SOUTH -> {
                uT = safeRatio(x - xMin, xSize);
                vT = safeRatio(y - yMin, ySize);
            }
            case WEST -> {
                uT = safeRatio(zMax - z, zSize);
                vT = safeRatio(y - yMin, ySize);
            }
            case EAST -> {
                uT = safeRatio(z - zMin, zSize);
                vT = safeRatio(y - yMin, ySize);
            }
            case UP -> {
                uT = safeRatio(x - xMin, xSize);
                vT = safeRatio(zMax - z, zSize);
            }
            case DOWN -> {
                uT = safeRatio(x - xMin, xSize);
                vT = safeRatio(z - zMin, zSize);
            }
        }

        float u = lerp(uMin, uMax, uT);
        float v = lerp(vMax, vMin, vT);

        int rotation = Math.floorMod(face.rotation(), 360);
        if (rotation != 0) {
            float[] rotated = rotateUV(u, v, uMin, uMax, vMin, vMax, rotation);
            u = rotated[0];
            v = rotated[1];
        }

        return new float[]{u, v};
    }

    private float safeRatio(float value, float size) {
        if (Math.abs(size) <= 1e-6f) {
            return 0.0f;
        }
        return value / size;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float[] rotateUV(float u, float v,
                             float uMin, float uMax,
                             float vMin, float vMax,
                             int rotation) {
        float du = uMax - uMin;
        float dv = vMax - vMin;
        if (Math.abs(du) <= 1e-6f || Math.abs(dv) <= 1e-6f) {
            return new float[]{u, v};
        }

        float uN = (u - uMin) / du;
        float vN = (v - vMin) / dv;

        float ru = uN;
        float rv = vN;

        switch (rotation) {
            case 90 -> {
                ru = 1.0f - vN;
                rv = uN;
            }
            case 180 -> {
                ru = 1.0f - uN;
                rv = 1.0f - vN;
            }
            case 270 -> {
                ru = vN;
                rv = 1.0f - uN;
            }
            default -> {
            }
        }

        return new float[]{uMin + ru * du, vMin + rv * dv};
    }

    private void addVertex(VertexConsumer buffer, PoseStack.Pose pose,
                           float x, float y, float z, float u, float v,
                           Vector3f normal, int packedLight, int packedOverlay) {
        Vector3f pos = new Vector3f(x, y, z);
        pos.mulPosition(pose.pose());

        buffer.addVertex(pos.x, pos.y, pos.z)
                .setColor(1.0f, 1.0f, 1.0f, 1.0f)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(normal.x, normal.y, normal.z);
    }

    public Optional<String> pickHotspot(double mouseX, double mouseY,
                                        List<ClueModelParser.CubeData> allCubes,
                                        List<ClueModelParser.CubeData> hotspotCubes) {
        if (allCubes.isEmpty() || hotspotCubes.isEmpty()) {
            return Optional.empty();
        }

        String frontmost = null;
        float bestDepth = -Float.MAX_VALUE;

        for (ClueModelParser.CubeData cube : allCubes) {
            ScreenProjection projection = projectCube(cube);
            if (projection == null) {
                continue;
            }

            if (!pointInPolygon((float) mouseX, (float) mouseY, projection.hull())) {
                continue;
            }

            if (projection.maxDepth() > bestDepth) {
                bestDepth = projection.maxDepth();
                frontmost = cube.name();
            }
        }

        if (frontmost == null) {
            return Optional.empty();
        }

        for (ClueModelParser.CubeData cube : hotspotCubes) {
            if (cube.name().equals(frontmost)) {
                return Optional.of(frontmost);
            }
        }

        return Optional.empty();
    }

    public List<float[]> getCubeScreenHull(ClueModelParser.CubeData cube) {
        ScreenProjection projection = projectCube(cube);
        if (projection == null) {
            return List.of();
        }
        List<float[]> hull = new java.util.ArrayList<>();
        for (ScreenPoint point : projection.hull()) {
            hull.add(new float[]{point.x(), point.y()});
        }
        return hull;
    }

    private ScreenProjection projectCube(ClueModelParser.CubeData cube) {
        Vec3 from = cube.from();
        Vec3 to = cube.to();

        float xMin = (float) Math.min(from.x, to.x);
        float xMax = (float) Math.max(from.x, to.x);
        float yMin = (float) Math.min(from.y, to.y);
        float yMax = (float) Math.max(from.y, to.y);
        float zMin = (float) Math.min(from.z, to.z);
        float zMax = (float) Math.max(from.z, to.z);

        Vector3f[] corners = new Vector3f[] {
                new Vector3f(xMin, yMin, zMin),
                new Vector3f(xMin, yMin, zMax),
                new Vector3f(xMin, yMax, zMin),
                new Vector3f(xMin, yMax, zMax),
                new Vector3f(xMax, yMin, zMin),
                new Vector3f(xMax, yMin, zMax),
                new Vector3f(xMax, yMax, zMin),
                new Vector3f(xMax, yMax, zMax)
        };

        float rotXRad = (float) Math.toRadians(rotationX);
        float rotYRad = (float) Math.toRadians(rotationY);
        float scale = modelScale * zoom;

        Matrix4f transform = new Matrix4f()
                .translate(modelCenterX, modelCenterY, 0.0f)
                .scale(scale, -scale, scale)
                .rotateX(rotXRad)
                .rotateY(rotYRad);

        ClueModelParser.RotationData rotation = cube.rotation();
        if (rotation != null && rotation.angle() != 0) {
            Vec3 origin = rotation.origin();
            Matrix4f local = new Matrix4f()
                    .translate((float) origin.x, (float) origin.y, (float) origin.z);
            float angleRad = (float) Math.toRadians(rotation.angle());
            switch (rotation.axis()) {
                case X -> local.rotateX(angleRad);
                case Y -> local.rotateY(angleRad);
                case Z -> local.rotateZ(angleRad);
            }
            local.translate(-(float) origin.x, -(float) origin.y, -(float) origin.z);
            transform.mul(local);
        }

        List<ScreenPoint> points = new java.util.ArrayList<>(corners.length);
        float maxDepth = -Float.MAX_VALUE;

        for (Vector3f corner : corners) {
            transform.transformPosition(corner);
            float screenX = corner.x;
            float screenY = corner.y;
            float depth = corner.z;
            maxDepth = Math.max(maxDepth, depth);
            points.add(new ScreenPoint(screenX, screenY, depth));
        }

        List<ScreenPoint> hull = computeHull(points);
        if (hull.isEmpty()) {
            return null;
        }

        return new ScreenProjection(hull, maxDepth);
    }

    private List<ScreenPoint> computeHull(List<ScreenPoint> points) {
        if (points.size() <= 2) {
            return points;
        }

        List<ScreenPoint> sorted = new java.util.ArrayList<>(points);
        sorted.sort((a, b) -> {
            int cmp = Float.compare(a.x(), b.x());
            if (cmp != 0) {
                return cmp;
            }
            return Float.compare(a.y(), b.y());
        });

        List<ScreenPoint> lower = new java.util.ArrayList<>();
        for (ScreenPoint p : sorted) {
            while (lower.size() >= 2 && cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), p) <= 0) {
                lower.remove(lower.size() - 1);
            }
            lower.add(p);
        }

        List<ScreenPoint> upper = new java.util.ArrayList<>();
        for (int i = sorted.size() - 1; i >= 0; i--) {
            ScreenPoint p = sorted.get(i);
            while (upper.size() >= 2 && cross(upper.get(upper.size() - 2), upper.get(upper.size() - 1), p) <= 0) {
                upper.remove(upper.size() - 1);
            }
            upper.add(p);
        }

        lower.remove(lower.size() - 1);
        upper.remove(upper.size() - 1);
        lower.addAll(upper);
        return lower;
    }

    private float cross(ScreenPoint a, ScreenPoint b, ScreenPoint c) {
        float abx = b.x() - a.x();
        float aby = b.y() - a.y();
        float acx = c.x() - a.x();
        float acy = c.y() - a.y();
        return abx * acy - aby * acx;
    }

    private boolean pointInPolygon(float x, float y, List<ScreenPoint> polygon) {
        if (polygon.size() < 3) {
            return false;
        }

        boolean hasPos = false;
        boolean hasNeg = false;

        for (int i = 0; i < polygon.size(); i++) {
            ScreenPoint a = polygon.get(i);
            ScreenPoint b = polygon.get((i + 1) % polygon.size());
            float cross = (b.x() - a.x()) * (y - a.y()) - (b.y() - a.y()) * (x - a.x());
            if (cross < 0) {
                hasNeg = true;
            } else if (cross > 0) {
                hasPos = true;
            }
            if (hasNeg && hasPos) {
                return false;
            }
        }

        return true;
    }

    private record ScreenPoint(float x, float y, float z) {}
    private record ScreenProjection(List<ScreenPoint> hull, float maxDepth) {}

}
