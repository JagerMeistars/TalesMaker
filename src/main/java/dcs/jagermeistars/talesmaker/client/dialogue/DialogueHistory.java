package dcs.jagermeistars.talesmaker.client.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dcs.jagermeistars.talesmaker.TalesMaker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DialogueHistory {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<HistoryEntry> history = new ArrayList<>();
    private static String currentWorldId = null;

    public record HistoryEntry(
            String npcName,
            String icon,
            String message,
            long timestamp
    ) {}

    public static void addEntry(Component npcName, ResourceLocation icon, Component message) {
        HistoryEntry entry = new HistoryEntry(
                Component.Serializer.toJson(npcName, Minecraft.getInstance().level.registryAccess()),
                icon != null ? icon.toString() : "",
                Component.Serializer.toJson(message, Minecraft.getInstance().level.registryAccess()),
                System.currentTimeMillis()
        );
        history.add(entry);
        save();
    }

    public static List<HistoryEntry> getHistory() {
        return new ArrayList<>(history);
    }

    public static void clear() {
        history.clear();
        save();
    }

    public static void onWorldJoin(String worldId) {
        currentWorldId = worldId;
        load();
    }

    public static void onWorldLeave() {
        save();
        history.clear();
        currentWorldId = null;
    }

    private static Path getHistoryFile() {
        if (currentWorldId == null) {
            return null;
        }
        Path configDir = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("talesmaker")
                .resolve("history");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            TalesMaker.LOGGER.error("Failed to create history directory", e);
        }
        return configDir.resolve(currentWorldId + ".json");
    }

    private static void save() {
        Path file = getHistoryFile();
        if (file == null) {
            return;
        }
        try {
            String json = GSON.toJson(history);
            Files.writeString(file, json);
        } catch (IOException e) {
            TalesMaker.LOGGER.error("Failed to save dialogue history", e);
        }
    }

    private static void load() {
        Path file = getHistoryFile();
        if (file == null || !Files.exists(file)) {
            history.clear();
            return;
        }
        try {
            String json = Files.readString(file);
            Type listType = new TypeToken<List<HistoryEntry>>() {}.getType();
            List<HistoryEntry> loaded = GSON.fromJson(json, listType);
            history.clear();
            if (loaded != null) {
                history.addAll(loaded);
            }
        } catch (Exception e) {
            TalesMaker.LOGGER.error("Failed to load dialogue history", e);
            history.clear();
        }
    }

    public static Component parseNpcName(HistoryEntry entry) {
        try {
            return Component.Serializer.fromJson(entry.npcName(), Minecraft.getInstance().level.registryAccess());
        } catch (Exception e) {
            return Component.literal("???");
        }
    }

    public static Component parseMessage(HistoryEntry entry) {
        try {
            return Component.Serializer.fromJson(entry.message(), Minecraft.getInstance().level.registryAccess());
        } catch (Exception e) {
            return Component.literal("???");
        }
    }

    public static ResourceLocation parseIcon(HistoryEntry entry) {
        if (entry.icon() == null || entry.icon().isEmpty()) {
            return null;
        }
        try {
            return ResourceLocation.parse(entry.icon());
        } catch (Exception e) {
            return null;
        }
    }
}
