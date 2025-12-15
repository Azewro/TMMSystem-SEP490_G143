package tmmsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to broadcast real-time events via WebSocket.
 * Use this service in other services to notify connected clients of data changes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notify all clients that an order has been updated.
     * Clients subscribed to /topic/orders will receive this.
     * 
     * @param orderId The ID of the updated order
     * @param eventType Type of event (e.g., "STATUS_CHANGED", "STAGE_STARTED", "QC_COMPLETED")
     */
    public void notifyOrderUpdate(Long orderId, String eventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", eventType);
        payload.put("orderId", orderId);
        payload.put("timestamp", LocalDateTime.now().toString());
        
        log.debug("Broadcasting order update: orderId={}, eventType={}", orderId, eventType);
        messagingTemplate.convertAndSend("/topic/orders", payload);
    }

    /**
     * Notify all clients that a stage has been updated.
     * Clients subscribed to /topic/stages will receive this.
     * 
     * @param stageId The ID of the updated stage
     * @param orderId The ID of the parent order
     * @param eventType Type of event
     */
    public void notifyStageUpdate(Long stageId, Long orderId, String eventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", eventType);
        payload.put("stageId", stageId);
        payload.put("orderId", orderId);
        payload.put("timestamp", LocalDateTime.now().toString());
        
        log.debug("Broadcasting stage update: stageId={}, orderId={}, eventType={}", stageId, orderId, eventType);
        messagingTemplate.convertAndSend("/topic/stages", payload);
        
        // Also notify order topic since stage changes affect order status
        notifyOrderUpdate(orderId, "STAGE_" + eventType);
    }

    /**
     * Notify all clients that defects have been updated.
     * 
     * @param defectId The ID of the updated defect
     * @param eventType Type of event
     */
    public void notifyDefectUpdate(Long defectId, String eventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", eventType);
        payload.put("defectId", defectId);
        payload.put("timestamp", LocalDateTime.now().toString());
        
        log.debug("Broadcasting defect update: defectId={}, eventType={}", defectId, eventType);
        messagingTemplate.convertAndSend("/topic/defects", payload);
    }

    /**
     * Broadcast a general refresh signal to all list pages.
     * Use this when multiple entities may have changed.
     */
    public void broadcastRefresh() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "REFRESH_ALL");
        payload.put("timestamp", LocalDateTime.now().toString());
        
        log.debug("Broadcasting refresh all signal");
        messagingTemplate.convertAndSend("/topic/orders", payload);
        messagingTemplate.convertAndSend("/topic/stages", payload);
        messagingTemplate.convertAndSend("/topic/defects", payload);
    }
}
