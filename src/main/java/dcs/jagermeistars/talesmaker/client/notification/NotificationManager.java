package dcs.jagermeistars.talesmaker.client.notification;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationManager {
    private static final List<Notification> notifications = new ArrayList<>();
    private static final int MAX_NOTIFICATIONS = 10;
    private static final long DEFAULT_DURATION = 5000; // 5 seconds
    public static final long FADE_DURATION = 800; // 0.8 seconds fade out (smoother)
    public static final long APPEAR_DURATION = 200; // 0.2 seconds appear animation

    public enum NotificationType {
        SUCCESS(0x00AA00),  // Green
        WARNING(0xFFAA00),  // Yellow/Orange
        ERROR(0xDD0000);    // Red

        public final int color;

        NotificationType(int color) {
            this.color = color;
        }
    }

    public static class Notification {
        private final String message;
        private final NotificationType type;
        private final long startTime;
        private final long duration;
        private int count;

        public Notification(String message, NotificationType type, long startTime, long duration) {
            this.message = message;
            this.type = type;
            this.startTime = startTime;
            this.duration = duration;
            this.count = 1;
        }

        public String message() { return message; }
        public NotificationType type() { return type; }
        public long startTime() { return startTime; }
        public long duration() { return duration; }
        public int count() { return count; }

        public void incrementCount() {
            this.count++;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > startTime + duration + FADE_DURATION;
        }

        // Progress of appear animation (0 = just started, 1 = fully visible)
        public float getAppearProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= APPEAR_DURATION) {
                return 1f;
            }
            return elapsed / (float) APPEAR_DURATION;
        }

        // Progress of fade animation (0 = not fading, 1 = fully faded)
        public float getFadeProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            long fadeStart = duration;
            if (elapsed < fadeStart) {
                return 0f;
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
            // Check for existing notification with same message
            for (Notification n : notifications) {
                if (n.message().equals(message) && n.type() == type) {
                    n.incrementCount();
                    return;
                }
            }

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
