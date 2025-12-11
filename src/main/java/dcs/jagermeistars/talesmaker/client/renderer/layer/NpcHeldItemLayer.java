package dcs.jagermeistars.talesmaker.client.renderer.layer;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.data.ItemSlotsConfig;
import dcs.jagermeistars.talesmaker.data.NpcPreset;
import dcs.jagermeistars.talesmaker.entity.NpcEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Render layer for displaying held items in NPC hands.
 * Uses GeckoLib's BlockAndItemGeoLayer to attach items to bone positions.
 * Bone names are configurable per-preset via itemSlots configuration.
 */
public class NpcHeldItemLayer extends BlockAndItemGeoLayer<NpcEntity> {

    private static boolean debugLogged = false;

    public NpcHeldItemLayer(GeoRenderer<NpcEntity> renderer) {
        super(renderer);
    }

    /**
     * Get the item slots configuration for the given NPC.
     * Falls back to defaults if preset or config is not available.
     */
    private ItemSlotsConfig getItemSlots(NpcEntity npc) {
        NpcPreset preset = npc.getPreset();
        if (preset != null && preset.itemSlots() != null) {
            return preset.itemSlots();
        }
        return ItemSlotsConfig.DEFAULT;
    }

    @Override
    @Nullable
    protected ItemStack getStackForBone(GeoBone bone, NpcEntity animatable) {
        String boneName = bone.getName();
        ItemSlotsConfig slots = getItemSlots(animatable);

        // Debug logging (only once to avoid spam)
        if (!debugLogged) {
            ItemStack mainhand = animatable.getMainHandItemStack();
            ItemStack offhand = animatable.getOffhandItemStack();
            
            if (boneName.equals("right_item") || boneName.equals("left_item")) {
                debugLogged = true;
            }
        }

        if (slots.isMainhand(boneName)) {
            return animatable.getMainHandItemStack();
        } else if (slots.isOffhand(boneName)) {
            return animatable.getOffhandItemStack();
        }

        return null;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, NpcEntity animatable) {
        String boneName = bone.getName();
        ItemSlotsConfig slots = getItemSlots(animatable);

        // Use RIGHT_HAND context for both hands since bone positions are already mirrored in the model
        // Using LEFT_HAND would apply additional mirroring that conflicts with the model's bone setup
        if (slots.isMainhand(boneName) || slots.isOffhand(boneName)) {
            return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        }

        return ItemDisplayContext.NONE;
    }
}
