package com.zyelectric.ocpp.config;

import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.repository.ChargeBoxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Enforces OCPP 1.6 Security Profile 1: HTTP Basic Auth (chargeBoxId:password) on the
 * WebSocket upgrade. A charger must be pre-registered with a password (see
 * ChargerController#createCharger) before it can connect.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcppHandshakeInterceptor implements HandshakeInterceptor {

    private final ChargeBoxRepository chargeBoxRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String chargePointName = lastPathSegment(request);
        if (chargePointName == null || chargePointName.isBlank()) {
            reject(response);
            return false;
        }

        Optional<ChargeBox> chargeBox = chargeBoxRepository.findByChargeBoxId(chargePointName);
        if (chargeBox.isEmpty() || chargeBox.get().getPasswordHash() == null) {
            log.warn("Rejected WebSocket handshake for unknown/unprovisioned charger '{}'", chargePointName);
            reject(response);
            return false;
        }

        BasicCredentials credentials = extractBasicCredentials(request);
        if (credentials == null
                || !credentials.username().equals(chargePointName)
                || !passwordEncoder.matches(credentials.password(), chargeBox.get().getPasswordHash())) {
            log.warn("Rejected WebSocket handshake for charger '{}': invalid credentials", chargePointName);
            reject(response);
            return false;
        }

        respondWithOcppSubprotocol(request, response);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private void reject(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"ocpp\"");
    }

    private void respondWithOcppSubprotocol(ServerHttpRequest request, ServerHttpResponse response) {
        List<String> subprotocols = request.getHeaders().get("Sec-WebSocket-Protocol");
        if (subprotocols != null && subprotocols.contains("ocpp1.6")) {
            response.getHeaders().add("Sec-WebSocket-Protocol", "ocpp1.6");
        }
    }

    private String lastPathSegment(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        String[] parts = path.split("/");
        return parts.length == 0 ? null : parts[parts.length - 1];
    }

    private BasicCredentials extractBasicCredentials(ServerHttpRequest request) {
        List<String> headers = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (headers == null || headers.isEmpty() || !headers.get(0).startsWith("Basic ")) {
            return null;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(headers.get(0).substring(6)), StandardCharsets.UTF_8);
            int sep = decoded.indexOf(':');
            if (sep < 0) {
                return null;
            }
            return new BasicCredentials(decoded.substring(0, sep), decoded.substring(sep + 1));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private record BasicCredentials(String username, String password) {
    }
}
