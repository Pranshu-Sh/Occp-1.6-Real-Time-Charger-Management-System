package com.zyelectric.ocpp.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyelectric.ocpp.cache.WebSocketSessionCache;
import com.zyelectric.ocpp.service.OcppMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcppWebSocketConnectionHandler implements WebSocketHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final OcppMessageProcessor messageProcessor;
    private final ThreadPoolTaskExecutor executor;  // ✅ Your 100-thread pool

    // Map to store message queues per charge point
    private final Map<String, Queue<String>> messageQueues = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        executor.execute(() -> {
            String chargePointName = getChargePointName(session);

            if (chargePointName == null) {
                log.error("No charge point name found. Closing session.");
                closeSession(session, CloseStatus.BAD_DATA);
                return;
            }

            WebSocketSessionCache.addSession(chargePointName, session);

            // Create a message queue for this charge point if it doesn't exist
            messageQueues.putIfAbsent(chargePointName, new ConcurrentLinkedQueue<>());

            log.info("OCPP connection established for {}", chargePointName);
        });
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        executor.execute(() -> {
            String requestMessage = (String) message.getPayload();
            String chargePointName = getChargePointName(session);

            if (chargePointName == null) {
                log.error("Charge point name not found. Closing session.");
                closeSession(session, CloseStatus.BAD_DATA);
                return;
            }

            log.info("Received message from {}: {}", chargePointName, requestMessage);

            // Queue the message and process the queue sequentially
            messageQueues.get(chargePointName).add(requestMessage);
            processMessageQueue(chargePointName, session);
        });
    }

    private void processMessageQueue(String chargePointName, WebSocketSession session) {
        executor.execute(() -> {
            Queue<String> queue = messageQueues.get(chargePointName);

            if (queue == null || queue.isEmpty()) {
                return;
            }

            while (!queue.isEmpty()) {
                String message = queue.poll();
                if (message != null) {

                    if (session.isOpen()) {
                        messageProcessor.processMessage(chargePointName, message, session);
                        log.info("Sent message to {}: {}", chargePointName, message);
                    } else {
                        log.warn("Session closed. Requeuing message.");
                        queue.add(message);
                    }
                }
            }
        });
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Transport error: {}", exception.getMessage());
        executor.execute(() -> closeSession(session, CloseStatus.SERVER_ERROR));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        executor.execute(() -> {
            String chargePointName = getChargePointName(session);

            if (chargePointName != null) {
                log.info("Connection closed for {} - status: {}", chargePointName, closeStatus);

                try {
                    if (session.isOpen()) {
                        session.close(CloseStatus.NORMAL);
                        log.info("Force closed stale session for {}", chargePointName);
                    }
                } catch (IOException e) {
                    log.warn("Failed to force close session for {}: {}", chargePointName, e.getMessage());
                }

                WebSocketSessionCache.removeSession(chargePointName);
                messageQueues.remove(chargePointName);  // Cleanup queue
                log.info("Removed session and message queue for {}", chargePointName);
            }
        });
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private void closeSession(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException e) {
            log.error("Failed to close session: {}", e.getMessage());
        }
    }

    private String getChargePointName(WebSocketSession session) {
        return Optional.ofNullable(session.getUri())
                .map(uri -> uri.getPath().split("/"))
                .map(parts -> parts[parts.length - 1])
                .orElse(null);
    }
}
