package com.zyelectric.ocpp.config;

import com.zyelectric.ocpp.handler.OcppWebSocketConnectionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final OcppWebSocketConnectionHandler ocppWebSocketConnectionHandler;
    private final OcppHandshakeInterceptor ocppHandshakeInterceptor;

    @Value("${app.allowed-origins:}")
    private String allowedOrigins;

    @Value("${app.websocket.idle-timeout-ms:1200000}")
    private long idleTimeoutMs;

    /**
     * Each session gets its own dedicated processing thread (see
     * OcppWebSocketConnectionHandler); a session that never sends a clean TCP close
     * (common on flaky/cellular charger links) would otherwise hold that thread forever.
     * Bounding idle time here ensures the container proactively closes it, triggering
     * afterConnectionClosed cleanup instead of leaking a thread indefinitely.
     *
     * Excluded from the "test" profile: it configures the real embedded servlet
     * container's WebSocket support, which MockMvc's mock servlet environment doesn't
     * provide - there's no real container there to tune.
     */
    @Bean
    @Profile("!test")
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxSessionIdleTimeout(idleTimeoutMs);
        return container;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        var registration = registry
                .addHandler(ocppWebSocketConnectionHandler, "/{chargePointName}")
                .addInterceptors(ocppHandshakeInterceptor);

        // Only browser clients send an Origin header at all; OCPP charge points don't,
        // so leaving this unset (same-origin-only default) never blocks real hardware.
        // Configure ALLOWED_ORIGINS only if a browser-based admin UI needs to open this
        // socket directly from a different origin.
        if (!allowedOrigins.isBlank()) {
            registration.setAllowedOrigins(allowedOrigins.split(","));
        }
    }
}
