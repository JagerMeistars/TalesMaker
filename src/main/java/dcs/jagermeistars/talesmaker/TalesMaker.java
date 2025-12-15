package dcs.jagermeistars.talesmaker;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dcs.jagermeistars.talesmaker.command.TalesMakerCommands;
import dcs.jagermeistars.talesmaker.data.NpcPresetManager;
import dcs.jagermeistars.talesmaker.data.choice.ChoiceWindowManager;
import dcs.jagermeistars.talesmaker.entity.NpcEntity;
import dcs.jagermeistars.talesmaker.init.ModEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(TalesMaker.MODID)
public class TalesMaker {
    public static final String MODID = "talesmaker";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final NpcPresetManager PRESET_MANAGER = new NpcPresetManager();
    public static final ChoiceWindowManager CHOICE_MANAGER = new ChoiceWindowManager();

    public TalesMaker(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onEntityAttributeCreation);

        // Register entities
        ModEntities.ENTITIES.register(modEventBus);

        // Register for server events
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Common setup logic
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.NPC.get(), NpcEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Server starting logic
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TalesMakerCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(PRESET_MANAGER);
        event.addListener(CHOICE_MANAGER);
    }
}
