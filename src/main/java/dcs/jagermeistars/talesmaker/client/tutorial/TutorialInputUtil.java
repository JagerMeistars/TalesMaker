package dcs.jagermeistars.talesmaker.client.tutorial;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class TutorialInputUtil {

    private TutorialInputUtil() {}

    public static boolean matchesAdvanceKey(String keyName, int code, boolean mouse) {
        if (keyName == null || keyName.isEmpty()) {
            return false;
        }
        InputConstants.Key key;
        try {
            key = InputConstants.getKey(keyName);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (mouse && key.getType() == InputConstants.Type.MOUSE) {
            return key.getValue() == code;
        }
        if (!mouse && key.getType() == InputConstants.Type.KEYSYM) {
            return key.getValue() == code;
        }
        return false;
    }

    public static boolean isAdvanceKeyDown(String keyName) {
        if (keyName == null || keyName.isEmpty()) {
            return false;
        }
        InputConstants.Key key;
        try {
            key = InputConstants.getKey(keyName);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(window, key.getValue());
        }
        return false;
    }
}
