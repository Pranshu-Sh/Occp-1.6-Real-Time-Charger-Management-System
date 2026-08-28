package com.zyelectric.ocpp.cache;

import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

public final class WebSocketSessionCache {

    private static final ConcurrentHashMap<String, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    private WebSocketSessionCache() {
        // Private constructor to prevent instantiation
    }

    public static void addSession(String chargePointName, WebSocketSession session) {
        SESSION_MAP.put(chargePointName, session);
    }

    /**
     * Removes whatever session is currently cached for this charger, unconditionally.
     * Prefer {@link #removeSession(String, WebSocketSession)} when closing a specific
     * connection - this variant can delete a healthy reconnected session's entry if a
     * stale connection's close event arrives late.
     */
    public static void removeSession(String chargePointName) {
        SESSION_MAP.remove(chargePointName);
    }

    /**
     * Removes the cached session only if it is still exactly this instance - a session
     * whose close event arrives after the charger has already reconnected (a new session
     * registered under the same name) will not evict the new, live session.
     */
    public static void removeSession(String chargePointName, WebSocketSession session) {
        SESSION_MAP.remove(chargePointName, session);
    }

    public static WebSocketSession getSessionData(String chargePointName) {
        return SESSION_MAP.get(chargePointName);
    }

    public static void clearAllSessions() {
        SESSION_MAP.clear();
    }
}
