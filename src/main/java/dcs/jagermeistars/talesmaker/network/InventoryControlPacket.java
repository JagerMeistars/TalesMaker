package dcs.jagermeistars.talesmaker.network;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.client.inventory.ClientInventoryControl;
import dcs.jagermeistars.talesmaker.inventory.InventoryRestrictions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record InventoryControlPacket(
        boolean blockInventoryOpen,
        boolean hideHotbar,
        int hotbarSlots,
        int inventorySlots
) implements CustomPacketPayload {

    public static final Type<InventoryControlPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "inventory_control"));

    public static final StreamCodec<FriendlyByteBuf, InventoryControlPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, InventoryControlPacket::blockInventoryOpen,
            ByteBufCodecs.BOOL, InventoryControlPacket::hideHotbar,
            ByteBufCodecs.VAR_INT, InventoryControlPacket::hotbarSlots,
            ByteBufCodecs.VAR_INT, InventoryControlPacket::inventorySlots,
            InventoryControlPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static InventoryControlPacket fromRestrictions(InventoryRestrictions restrictions) {
        InventoryRestrictions normalized = restrictions.normalized();
        return new InventoryControlPacket(
                normalized.blockInventoryOpen(),
                normalized.hideHotbar(),
                normalized.hotbarSlots(),
                normalized.inventorySlots()
        );
    }

    public static void handle(InventoryControlPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientInventoryControl.applyPacket(packet));
    }
}
