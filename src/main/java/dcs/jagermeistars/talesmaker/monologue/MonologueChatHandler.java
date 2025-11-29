package dcs.jagermeistars.talesmaker.monologue;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.data.NpcPreset;
import dcs.jagermeistars.talesmaker.network.DialoguePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = TalesMaker.MODID)
public class MonologueChatHandler {

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();

        if (!MonologueManager.hasMonologue(player.getUUID())) {
            return;
        }

        NpcPreset preset = MonologueManager.getPreset(player.getUUID());
        if (preset == null) {
            return;
        }

        // Cancel the chat message
        event.setCanceled(true);

        String iconPath = preset.icon() != null ? preset.icon().toString() : "";

        // Send dialogue packet to all players
        DialoguePacket packet = new DialoguePacket(
                preset.name(),
                iconPath,
                Component.literal(event.getRawText())
        );

        PacketDistributor.sendToAllPlayers(packet);
    }
}
