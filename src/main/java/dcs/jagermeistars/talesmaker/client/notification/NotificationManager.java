package dcs.jagermeistars.talesmaker.client.notification;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationManager {
    private static final List<Notification> notifications = new ArrayList<>();
    private static final int MAX_NOTIFICATIONS = 10;
    private static final long DEFAULT_DURATION = 5000; // 5 seconds
    private static final long FADE_DURATION = 500; // 0.5 seconds fade out

    public enum NotificationType {
        SUCCESS(0x00AA00),  // Green
        WARNING(0xFFAA00),  // Yellow/Orange
        ERROR(0xDD0000);    // Red

        public final int color;

        NotificationType(int color) {
            this.color = color;
        }
    }

    public record Notification(String message, NotificationType type, long startTime, long duration) {
        public boolean isExpired() {
            return System.currentTimeMillis() > startTime + duration + FADE_DURATION;
        }

        public float getFadeProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            long fadeStart = duration;
            if (elapsed < fadeStart) {
                return 0f; // Not fading yet
            }
            float progress = (elapsed - fadeStart) / (float) FADE_DURATION;
            return Math.min(1f, progress);
        }

        public boolean isFading() {
            long elapsed = System.currentTimeMillis() - startTime;
            return elapsed >= duration;
        }
    }

    public static void addNotification(String message, NotificationType type) {
        addNotification(message, type, DEFAULT_DURATION);
    }

    public static void addNotification(String message, NotificationType type, long durationMs) {
        synchronized (notifications) {
            notifications.add(new Notification(message, type, System.currentTimeMillis(), durationMs));
            // Limit notifications
            while (notifications.size() > MAX_NOTIFICATIONS) {
                notifications.remove(0);
            }
        }
    }

    public static void success(String message) {
        addNotification(message, NotificationType.SUCCESS);
    }

    public static void warning(String message) {
        addNotification(message, NotificationType.WARNING);
    }

    public static void error(String message) {
        addNotification(message, NotificationType.ERROR);
    }

    public static List<Notification> getNotifications() {
        synchronized (notifications) {
            // Remove expired notifications
            Iterator<Notification> it = notifications.iterator();
            while (it.hasNext()) {
                if (it.next().isExpired()) {
                    it.remove();
                }
            }
            return new ArrayList<>(notifications);
        }
    }

    public static void clear() {
        synchronized (notifications) {
            notifications.clear();
        }
    }
}
