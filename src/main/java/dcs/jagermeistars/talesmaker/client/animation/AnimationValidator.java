package dcs.jagermeistars.talesmaker.client.animation;

import dcs.jagermeistars.talesmaker.client.notification.ResourceErrorManager;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.loading.object.BakedAnimations;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates animation names against loaded animation files.
 * Reports errors for missing animations via ResourceErrorManager.
 */
public class AnimationValidator {

    // Track which animations have already been reported to prevent spam
    private static final Set<String> REPORTED_ANIMATIONS = Collections.synchronizedSet(new HashSet<>());

    /**
     * Clear validation cache. Called on /reload.
     */
    public static void clearCache() {
        REPORTED_ANIMATIONS.clear();
    }

    /**
     * Validate that an animation name exists in the given animation file.
     * Reports an error if the animation is not found.
     *
     * @param animationFile The animation file ResourceLocation
     * @param animationName The animation name to validate
     * @return true if animation exists, false otherwise
     */
    public static boolean validateAnimation(ResourceLocation animationFile, String animationName) {
        if (animationFile == null || animationName == null || animationName.isEmpty()) {
            return false;
        }

        // Check GeckoLib cache for the animation file
        BakedAnimations bakedAnimations = GeckoLibCache.getBakedAnimations().get(animationFile);
        if (bakedAnimations == null) {
            // Animation file not loaded - this would be caught by ANIMATION type error
            return false;
        }

        // Check if the specific animation name exists
        if (bakedAnimations.getAnimation(animationName) == null) {
            // Animation name not found - report error once
            String reportKey = animationFile.toString() + ":" + animationName;
            if (REPORTED_ANIMATIONS.add(reportKey)) {
                ResourceErrorManager.addAnimationNameError(animationName, animationFile);
            }
            return false;
        }

        return true;
    }

    /**
     * Generate a unique report key for tracking reported errors.
     */
    private static String getReportKey(ResourceLocation animationFile, String animationName) {
        return animationFile.toString() + ":" + animationName;
    }
}
