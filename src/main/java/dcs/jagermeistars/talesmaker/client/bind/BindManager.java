package dcs.jagermeistars.talesmaker.client.bind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class BindManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Integer, BindEntry> BINDS_BY_KEYCODE = new HashMap<>();
    private static final Map<String, BindEntry> BINDS_BY_NAME = new HashMap<>();
    private static final Map<Integer, Boolean> LAST_PRESSED = new HashMap<>();
    private static List<String> ALL_KEYS = null;
    private static boolean loaded = false;

    private BindManager() {}

    public enum BindAddResult {
        OK,
        INVALID_KEY,
        USED
    }

    public record BindEntry(String key, String command, int keyCode) {}

    private record StoredBind(String key, String command) {}

    public static void load() {
        BINDS_BY_KEYCODE.clear();
        BINDS_BY_NAME.clear();
        LAST_PRESSED.clear();
        loaded = true;

        Path file = getBindFile();
        if (file == null || !Files.exists(file)) {
            return;
        }
        try {
            String json = Files.readString(file);
            Type listType = new TypeToken<List<StoredBind>>() {}.getType();
            List<StoredBind> stored = GSON.fromJson(json, listType);
            if (stored != null) {
                for (StoredBind bind : stored) {
                    addBindInternal(bind.key(), bind.command(), false);
                }
            }
        } catch (Exception ignored) {
            BINDS_BY_KEYCODE.clear();
            BINDS_BY_NAME.clear();
        }
    }

    public static void tick() {
        if (!loaded) {
            load();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        long window = mc.getWindow().getWindow();
        for (BindEntry entry : BINDS_BY_KEYCODE.values()) {
            boolean down = InputConstants.isKeyDown(window, entry.keyCode());
            boolean wasDown = LAST_PRESSED.getOrDefault(entry.keyCode(), false);
            if (down && !wasDown) {
                executeCommand(entry.command());
            }
            LAST_PRESSED.put(entry.keyCode(), down);
        }
    }

    public static BindAddResult addBind(String rawKey, String command) {
        return addBindInternal(rawKey, command, true);
    }

    public static boolean removeBind(String rawKey) {
        if (rawKey == null || rawKey.isEmpty()) {
            return false;
        }
        ParsedKey parsed = parseKey(rawKey);
        if (!parsed.valid()) {
            return false;
        }
        BindEntry removed = BINDS_BY_KEYCODE.remove(parsed.keyCode());
        if (removed != null) {
            BINDS_BY_NAME.remove(removed.key());
            LAST_PRESSED.remove(parsed.keyCode());
            save();
            return true;
        }
        return false;
    }

    public static List<String> getBindKeyNames() {
        return new ArrayList<>(BINDS_BY_NAME.keySet());
    }

    public static List<String> getAvailableKeyNames() {
        List<String> all = getAllKeyNames();
        List<String> available = new ArrayList<>();
        for (String keyName : all) {
            ParsedKey parsed = parseKey(keyName);
            if (parsed.valid() && isKeyAvailable(parsed.keyCode())) {
                available.add(keyName);
            }
        }
        return available;
    }

    public static List<String> getAllKeyNames() {
        if (ALL_KEYS != null) {
            return ALL_KEYS;
        }
        Set<String> keys = new LinkedHashSet<>();
        keys.add("key.mouse.left");
        keys.add("key.mouse.right");
        keys.add("key.mouse.middle");
        for (int code = GLFW.GLFW_KEY_SPACE; code <= GLFW.GLFW_KEY_LAST; code++) {
            InputConstants.Key key = InputConstants.getKey(code, -1);
            if (key == InputConstants.UNKNOWN) {
                continue;
            }
            String name = key.getName();
            if (name == null || name.isEmpty()) {
                continue;
            }
            if (name.contains("unknown")) {
                continue;
            }
            keys.add(name);
        }
        ALL_KEYS = new ArrayList<>(keys);
        return ALL_KEYS;
    }

    private static BindAddResult addBindInternal(String rawKey, String command, boolean save) {
        if (rawKey == null || rawKey.isEmpty() || command == null || command.isEmpty()) {
            return BindAddResult.INVALID_KEY;
        }
        ParsedKey parsed = parseKey(rawKey);
        if (!parsed.valid()) {
            return BindAddResult.INVALID_KEY;
        }
        if (!isKeyAvailable(parsed.keyCode())) {
            return BindAddResult.USED;
        }
        BindEntry entry = new BindEntry(parsed.keyName(), command, parsed.keyCode());
        BINDS_BY_KEYCODE.put(parsed.keyCode(), entry);
        BINDS_BY_NAME.put(parsed.keyName(), entry);
        if (save) {
            save();
        }
        return BindAddResult.OK;
    }

    private static boolean isKeyAvailable(int keyCode) {
        if (BINDS_BY_KEYCODE.containsKey(keyCode)) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return true;
        }
        for (KeyMapping mapping : mc.options.keyMappings) {
            if (mapping.getKey().getValue() == keyCode) {
                return false;
            }
        }
        return true;
    }

    private static void executeCommand(String command) {
        if (command == null || command.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) {
            return;
        }
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        if (normalized.isEmpty()) {
            return;
        }
        mc.player.connection.sendCommand(normalized);
    }

    private static void save() {
        Path file = getBindFile();
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            List<StoredBind> stored = new ArrayList<>();
            for (BindEntry entry : BINDS_BY_KEYCODE.values()) {
                stored.add(new StoredBind(entry.key(), entry.command()));
            }
            Files.writeString(file, GSON.toJson(stored));
        } catch (IOException ignored) {
        }
    }

    private static Path getBindFile() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameDirectory == null) {
            return null;
        }
        return mc.gameDirectory.toPath()
                .resolve("talesmaker")
                .resolve("binds.json");
    }

    private static ParsedKey parseKey(String rawKey) {
        String normalized = normalizeKeyName(rawKey);
        InputConstants.Key key = InputConstants.getKey(normalized);
        if (key == InputConstants.UNKNOWN) {
            return ParsedKey.invalid();
        }
        return new ParsedKey(normalized, key.getValue(), true);
    }

    private static String normalizeKeyName(String rawKey) {
        String key = rawKey.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("key.")) {
            return key;
        }
        if (key.startsWith("mouse.")) {
            return "key." + key;
        }
        if ("mouse1".equals(key) || "mouse_left".equals(key) || "left".equals(key)) {
            return "key.mouse.left";
        }
        if ("mouse2".equals(key) || "mouse_right".equals(key) || "right".equals(key)) {
            return "key.mouse.right";
        }
        if ("mouse3".equals(key) || "mouse_middle".equals(key) || "middle".equals(key)) {
            return "key.mouse.middle";
        }
        if (key.length() == 1) {
            return "key.keyboard." + key;
        }
        if (key.startsWith("f") && key.length() <= 3 && key.substring(1).chars().allMatch(Character::isDigit)) {
            return "key.keyboard." + key;
        }
        return "key.keyboard." + key;
    }

    private record ParsedKey(String keyName, int keyCode, boolean valid) {
        static ParsedKey invalid() {
            return new ParsedKey("", -1, false);
        }
    }

    public static void sendMessage(String message, boolean error) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        int color = error ? 0xFF5555 : 0x55FF55;
        mc.player.displayClientMessage(Component.literal(message).withStyle(style -> style.withColor(color)), false);
    }
}
