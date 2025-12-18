package tmmsystem.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

/**
 * Service to handle real-time messaging via WebSockets (STOMP).
 * Provides methods for private (per-user) notifications and global/role
 * broadcasts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send a private notification to a specific user.
     * Logic: Broadcasts to /user/{userId}/queue/notifications
     */
    public void sendNotification(Long userId, Object notification) {
        if (userId == null)
            return;
        try {
            log.info("Sending real-time notification to user: {}", userId);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Broadcast a data update event to all connected clients.
     * Pages use this to trigger a data refresh.
     */
    public void broadcastDataUpdate(String entity, Long id, String action) {
        try {
            log.info("Broadcasting data update: {} {} id={}", entity, action, id);
            Map<String, Object> payload = Map.of(
                    "entity", entity,
                    "id", id != null ? id : 0,
                    "action", action,
                    "timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSend("/topic/updates", payload);
        } catch (Exception e) {
            log.error("Failed to broadcast data update: {}", e.getMessage());
        }
    }

    /**
     * Broadcast to a specific role.
     */
    public void broadcastToRole(String role, Object payload) {
        if (role == null)
            return;
        try {
            String destination = "/topic/role/" + role.toUpperCase().replace(" ", "_");
            log.info("Broadcasting to role: {}", destination);
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.error("Failed to broadcast to role {}: {}", role, e.getMessage());
        }
    }
}
