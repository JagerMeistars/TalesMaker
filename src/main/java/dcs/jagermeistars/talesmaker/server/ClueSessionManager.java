package dcs.jagermeistars.talesmaker.server;

import dcs.jagermeistars.talesmaker.data.clue.CluePreset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Manages active clue inspection sessions for players.
 * Each player can have one active session at a time.
 * Sessions are transient and not persisted across server restarts.
 */
public class ClueSessionManager {

    /**
     * Represents an active clue inspection session.
     */
    public record ClueSession(
            ResourceLocation presetId,
            Set<String> discoveredClues,
            int totalClues
    ) {
        public boolean isComplete() {
            return discoveredClues.size() >= totalClues;
        }

        public int getDiscoveredCount() {
            return discoveredClues.size();
        }
    }

    // Map of player UUID to active session
    private static final Map<UUID, ClueSession> activeSessions = new HashMap<>();

    /**
     * Start a new clue inspection session for a player.
     * This will replace any existing session.
     *
     * @param player The player starting the session
     * @param preset The clue preset being inspected
     */
    public static void startSession(ServerPlayer player, CluePreset preset) {
        ClueSession session = new ClueSession(
                preset.id(),
                new HashSet<>(),
                preset.getClueCount()
        );
        activeSessions.put(player.getUUID(), session);
    }

    /**
     * Record a clue discovery for a player.
     *
     * @param player  The player who discovered the clue
     * @param clueId  The bone name of the discovered clue
     * @return true if this was a new discovery, false if already discovered
     */
    public static boolean discoverClue(ServerPlayer player, String clueId) {
        ClueSession session = activeSessions.get(player.getUUID());
        if (session == null) {
            return false;
        }
        return session.discoveredClues().add(clueId);
    }

    /**
     * Check if a player has completed their current session (found all clues).
     *
     * @param player The player to check
     * @return true if all clues have been found
     */
    public static boolean isComplete(ServerPlayer player) {
        ClueSession session = activeSessions.get(player.getUUID());
        return session != null && session.isComplete();
    }

    /**
     * Get the current session for a player.
     *
     * @param player The player
     * @return Optional containing the session if one exists
     */
    public static Optional<ClueSession> getSession(ServerPlayer player) {
        return Optional.ofNullable(activeSessions.get(player.getUUID()));
    }

    /**
     * End a player's current session.
     *
     * @param player The player whose session to end
     */
    public static void endSession(ServerPlayer player) {
        activeSessions.remove(player.getUUID());
    }

    /**
     * Check if a clue has been discovered in the current session.
     *
     * @param player  The player
     * @param clueId  The bone name of the clue
     * @return true if the clue has been discovered
     */
    public static boolean isClueDiscovered(ServerPlayer player, String clueId) {
        ClueSession session = activeSessions.get(player.getUUID());
        return session != null && session.discoveredClues().contains(clueId);
    }

    /**
     * Clean up session when player disconnects.
     *
     * @param player The player who disconnected
     */
    public static void onPlayerDisconnect(ServerPlayer player) {
        activeSessions.remove(player.getUUID());
    }
}
