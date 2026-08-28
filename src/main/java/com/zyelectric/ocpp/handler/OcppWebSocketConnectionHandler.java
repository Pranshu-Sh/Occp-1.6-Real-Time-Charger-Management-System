package com.zyelectric.ocpp.handler;

import com.zyelectric.ocpp.cache.WebSocketSessionCache;
import com.zyelectric.ocpp.service.OcppMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Each WebSocket session gets its own single-thread executor, so messages from the same
 * charger are always processed strictly in the order they arrive (FIFO), while different
 * chargers' messages still run fully in parallel across sessions. This replaces a previous
 * shared-thread-pool + manual queue design that could run two "drain" tasks for the same
 * charger concurrently, racing on message order, and could spin a thread indefinitely if a
 * session closed mid-drain.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcppWebSocketConnectionHandler implements WebSocketHandler {

    private final OcppMessageProcessor messageProcessor;
    private final Map<String, ExecutorService> sessionExecutors = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String chargePointName = getChargePointName(session);
        if (chargePointName == null) {
            log.error("No charge point name found. Closing session.");
            closeSession(session, CloseStatus.BAD_DATA);
            return;
        }

        WebSocketSessionCache.addSession(chargePointName, session);
        sessionExecutors.put(session.getId(),
                Executors.newSingleThreadExecutor(r -> new Thread(r, "ws-" + chargePointName)));

        log.info("OCPP connection established for {}", chargePointName);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        String chargePointName = getChargePointName(session);
        if (chargePointName == null) {
            log.error("Charge point name not found. Closing session.");
            closeSession(session, CloseStatus.BAD_DATA);
            return;
        }

        ExecutorService executor = sessionExecutors.get(session.getId());
        if (executor == null) {
            log.warn("No executor registered for session {} (charger {}); dropping message.", session.getId(), chargePointName);
            return;
        }

        String requestMessage = (String) message.getPayload();
        executor.submit(() -> {
            log.info("Received message from {}: {}", chargePointName, requestMessage);
            try {
                messageProcessor.processMessage(chargePointName, requestMessage, session);
            } catch (Exception e) {
                // OcppMessageProcessor already answers every message with a CallResult/CallError;
                // this is a last-resort guard so one bad message can never kill this session's
                // processing thread and silently stop future messages from being handled.
                log.error("Unhandled error processing message from {}: {}", chargePointName, e.getMessage(), e);
            }
        });
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Transport error: {}", exception.getMessage());
        closeSession(session, CloseStatus.SERVER_ERROR);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        String chargePointName = getChargePointName(session);
        if (chargePointName != null) {
            log.info("Connection closed for {} - status: {}", chargePointName, closeStatus);
            // Conditional remove: if this charger already reconnected under a new session
            // before this (possibly late/stale) close event arrived, don't evict the live one.
            WebSocketSessionCache.removeSession(chargePointName, session);
        }

        ExecutorService executor = sessionExecutors.remove(session.getId());
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
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
