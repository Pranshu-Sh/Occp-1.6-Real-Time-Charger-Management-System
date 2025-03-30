package com.zyelectric.ocpp.cache;

import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

public final class WebSocketSessionCache {

    // ✅ Store both WebSocketSession + TransactionContext in a single map
    private static final ConcurrentHashMap<String, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    private WebSocketSessionCache() {
        // Private constructor to prevent instantiation
    }

    /**
     * Adds a WebSocket session along with a transaction context to the cache.
     *
     * @param chargePointName the charge point name
     * @param session         the WebSocket session
     */
    public static void addSession(String chargePointName, WebSocketSession session) {
        SESSION_MAP.put(chargePointName, session);
    }

    /**
     * Removes a WebSocket session and transaction context from the cache.
     *
     * @param chargePointName the charge point name
     */
    public static void removeSession(String chargePointName) {
        SESSION_MAP.remove(chargePointName);
    }

    /**
     * Gets the session data (WebSocket + TransactionContext) by charge point name.
     *
     * @param chargePointName the charge point name
     * @return the SessionData or null if not found
     */
    public static WebSocketSession  getSessionData(String chargePointName) {
        return SESSION_MAP.get(chargePointName);
    }

    /**
     * Clears all sessions.
     */
    public static void clearAllSessions() {
        SESSION_MAP.clear();
    }
}
