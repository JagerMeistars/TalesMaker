package dcs.jagermeistars.talesmaker.client.notification;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages resource errors for GMod-style error display.
 * Shows detailed errors for missing models, textures, and animations.
 * Supports stacking of identical errors with count display.
 */
public class ResourceErrorManager {
    private static final List<ResourceError> errors = new ArrayList<>();
    private static final int MAX_ERRORS = 10;
    public static final long ERROR_DURATION = 10000; // 10 seconds
    public static final long FADE_DURATION = 800; // 0.8 seconds fade out
    public static final long APPEAR_DURATION = 200; // 0.2 seconds slide in

    public enum ResourceType {
        MODEL("error.talesmaker.missing_model"),
        TEXTURE("error.talesmaker.missing_texture"),
        ANIMATION("error.talesmaker.missing_animation"),
        /** Animation name not found in animation file */
        ANIMATION_NAME("error.talesmaker.missing_animation_name");

        private final String translationKey;

        ResourceType(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    public static class ResourceError {
        private final ResourceType type;
        private final ResourceLocation path;
        private final String extraInfo; // Additional info (e.g., animation name)
        private long startTime;
        private int count;

        public ResourceError(ResourceType type, ResourceLocation path, long startTime) {
            this(type, path, null, startTime);
        }

        public ResourceError(ResourceType type, ResourceLocation path, String extraInfo, long startTime) {
            this.type = type;
            this.path = path;
            this.extraInfo = extraInfo;
            this.startTime = startTime;
            this.count = 1;
        }

        public ResourceType type() { return type; }
        public ResourceLocation path() { return path; }
        public String extraInfo() { return extraInfo; }
        public long startTime() { return startTime; }
        public int count() { return count; }

        /**
         * Get display path string (includes extraInfo if present).
         */
        public String getDisplayPath() {
            if (extraInfo != null && !extraInfo.isEmpty()) {
                return path.toString() + " -> " + extraInfo;
            }
            return path.toString();
        }

        public void incrementCount() {
            this.count++;
            // Reset timer on increment so stacked errors stay visible
            this.startTime = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > startTime + ERROR_DURATION + FADE_DURATION;
        }

        /**
         * Progress of appear animation (0 = just started, 1 = fully visible)
         */
        public float getAppearProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= APPEAR_DURATION) {
                return 1f;
            }
            return elapsed / (float) APPEAR_DURATION;
        }

        /**
         * Progress of fade animation (0 = not fading, 1 = fully faded)
         */
        public float getFadeProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed < ERROR_DURATION) {
                return 0f;
            }
            float progress = (elapsed - ERROR_DURATION) / (float) FADE_DURATION;
            return Math.min(1f, progress);
        }

        /**
         * Check if this error matches another (same type, path, and extraInfo)
         */
        public boolean matches(ResourceType type, ResourceLocation path, String extraInfo) {
            if (this.type != type || !this.path.equals(path)) {
                return false;
            }
            // Compare extraInfo (both null or both equal)
            if (this.extraInfo == null) {
                return extraInfo == null;
            }
            return this.extraInfo.equals(extraInfo);
        }
    }

    /**
     * Adds a resource error. If same error already exists, increments its count.
     */
    public static void addError(ResourceType type, ResourceLocation path) {
        addError(type, path, null);
    }

    /**
     * Adds a resource error with extra info. If same error already exists, increments its count.
     */
    public static void addError(ResourceType type, ResourceLocation path, String extraInfo) {
        synchronized (errors) {
            // Check if same error already exists - stack it
            for (ResourceError error : errors) {
                if (error.matches(type, path, extraInfo)) {
                    error.incrementCount();
                    return;
                }
            }

            // New error
            errors.add(new ResourceError(type, path, extraInfo, System.currentTimeMillis()));

            // Limit errors
            while (errors.size() > MAX_ERRORS) {
                errors.remove(0);
            }
        }
    }

    /**
     * Adds an animation name error (animation name not found in file).
     */
    public static void addAnimationNameError(String animationName, ResourceLocation animationFile) {
        addError(ResourceType.ANIMATION_NAME, animationFile, animationName);
    }

    /**
     * Gets active (non-expired) errors.
     */
    public static List<ResourceError> getActiveErrors() {
        synchronized (errors) {
            // Remove expired errors
            Iterator<ResourceError> it = errors.iterator();
            while (it.hasNext()) {
                if (it.next().isExpired()) {
                    it.remove();
                }
            }
            return new ArrayList<>(errors);
        }
    }

    /**
     * Clears all errors and cache. Called on /reload and when player joins world.
     */
    public static void clearCache() {
        synchronized (errors) {
            errors.clear();
        }
    }

    /**
     * Checks if there are any active errors.
     */
    public static boolean hasErrors() {
        synchronized (errors) {
            return !errors.isEmpty();
        }
    }
}
