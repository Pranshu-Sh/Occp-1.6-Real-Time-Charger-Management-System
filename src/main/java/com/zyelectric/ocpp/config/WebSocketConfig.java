package com.zyelectric.ocpp.config;

import com.zyelectric.ocpp.handler.OcppWebSocketConnectionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final OcppWebSocketConnectionHandler ocppWebSocketConnectionHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(ocppWebSocketConnectionHandler, "/{chargePointName}")
                .setAllowedOrigins("*")
                .addInterceptors(customInterceptor());
    }
    @Bean
    public HandshakeInterceptor customInterceptor() {
        return new HandshakeInterceptor() {

            @Override
            public boolean beforeHandshake(
                    ServerHttpRequest request,
                    ServerHttpResponse response,
                    WebSocketHandler wsHandler,
                    Map<String, Object> attributes) {

                List<String> subprotocols = request.getHeaders().get("Sec-WebSocket-Protocol");
                if (subprotocols != null && subprotocols.contains("ocpp1.6")) {
                    response.getHeaders().add("Sec-WebSocket-Protocol", "ocpp1.6");
                }
                return true;
            }

            @Override
            public void afterHandshake(
                    ServerHttpRequest request,
                    ServerHttpResponse response,
                    WebSocketHandler wsHandler,
                    Exception exception) {
                log.info("Handshake completed with headers: {}", response.getHeaders());
            }
        };
    }

}
