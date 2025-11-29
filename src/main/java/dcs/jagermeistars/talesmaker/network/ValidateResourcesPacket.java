package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> Client: Request to validate resources for a preset
 */
public record ValidateResourcesPacket(
        String presetId,
        String modelPath,
        String texturePath,
        String emissivePath,
        String animationPath
) implements CustomPacketPayload {

    public static final Type<ValidateResourcesPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "validate_resources"));

    public static final StreamCodec<FriendlyByteBuf, ValidateResourcesPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ValidateResourcesPacket::presetId,
            ByteBufCodecs.STRING_UTF8, ValidateResourcesPacket::modelPath,
            ByteBufCodecs.STRING_UTF8, ValidateResourcesPacket::texturePath,
            ByteBufCodecs.STRING_UTF8, ValidateResourcesPacket::emissivePath,
            ByteBufCodecs.STRING_UTF8, ValidateResourcesPacket::animationPath,
            ValidateResourcesPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ValidateResourcesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            List<String> missingResources = new ArrayList<>();

            // Check model
            if (!packet.modelPath().isEmpty() && !resourceExists(packet.modelPath(), "model")) {
                missingResources.add("model: " + packet.modelPath());
            }

            // Check texture
            if (!packet.texturePath().isEmpty() && !resourceExists(packet.texturePath(), "texture")) {
                missingResources.add("texture: " + packet.texturePath());
            }

            // Check emissive (optional)
            if (!packet.emissivePath().isEmpty() && !resourceExists(packet.emissivePath(), "emissive")) {
                missingResources.add("emissive: " + packet.emissivePath());
            }

            // Check animation
            if (!packet.animationPath().isEmpty() && !resourceExists(packet.animationPath(), "animation")) {
                missingResources.add("animation: " + packet.animationPath());
            }

            // Send response back to server
            PacketDistributor.sendToServer(new ResourceValidationResponsePacket(
                    packet.presetId(),
                    missingResources
            ));
        });
    }

    private static boolean resourceExists(String path, String type) {
        try {
            ResourceLocation location = ResourceLocation.parse(path);
            ResourceLocation fullPath = switch (type) {
                case "model" -> ResourceLocation.fromNamespaceAndPath(
                        location.getNamespace(), "geo/" + location.getPath() + ".geo.json");
                case "texture" -> ResourceLocation.fromNamespaceAndPath(
                        location.getNamespace(), "textures/" + location.getPath() + ".png");
                case "emissive" -> ResourceLocation.fromNamespaceAndPath(
                        location.getNamespace(), "textures/" + location.getPath() + ".png");
                case "animation" -> ResourceLocation.fromNamespaceAndPath(
                        location.getNamespace(), "animations/" + location.getPath() + ".animation.json");
                default -> location;
            };
            return Minecraft.getInstance().getResourceManager().getResource(fullPath).isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}
