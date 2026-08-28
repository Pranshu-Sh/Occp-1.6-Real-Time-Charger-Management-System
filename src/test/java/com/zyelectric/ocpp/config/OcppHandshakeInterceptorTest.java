package com.zyelectric.ocpp.config;

import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.repository.ChargeBoxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Regression coverage for the highest-severity finding in the whole hardening effort: the
 * OCPP WebSocket endpoint previously had no authentication at all, so anyone could impersonate
 * any charger by dialing its URL. These tests assert the handshake now enforces Basic Auth
 * (chargeBoxId:password) against a pre-registered, provisioned charger.
 */
@ExtendWith(MockitoExtension.class)
class OcppHandshakeInterceptorTest {

    @Mock
    private ChargeBoxRepository chargeBoxRepository;
    @Mock
    private WebSocketHandler wsHandler;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private OcppHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new OcppHandshakeInterceptor(chargeBoxRepository, passwordEncoder);
    }

    private ServerHttpRequest request(String path, String authHeaderValue) throws Exception {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(new URI("ws://localhost:9093" + path));
        HttpHeaders headers = new HttpHeaders();
        if (authHeaderValue != null) {
            headers.add(HttpHeaders.AUTHORIZATION, authHeaderValue);
        }
        // lenient: some scenarios (unknown charger, no password provisioned) reject the
        // handshake before ever reading headers, so this stub isn't hit on every code path.
        lenient().when(request.getHeaders()).thenReturn(headers);
        return request;
    }

    private ServerHttpResponse response() {
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        lenient().when(response.getHeaders()).thenReturn(new HttpHeaders());
        return response;
    }

    private String basicAuthHeader(String user, String pass) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
    }

    private ChargeBox registeredCharger(String id, String rawPassword) {
        ChargeBox cb = new ChargeBox();
        cb.setChargeBoxId(id);
        cb.setPasswordHash(passwordEncoder.encode(rawPassword));
        return cb;
    }

    @Test
    void handshake_missingBasicAuth_rejected() throws Exception {
        when(chargeBoxRepository.findByChargeBoxId("CP001")).thenReturn(Optional.of(registeredCharger("CP001", "secret")));

        ServerHttpRequest request = request("/CP001", null);
        ServerHttpResponse response = response();

        boolean allowed = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

        assertThat(allowed).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handshake_wrongPassword_rejected() throws Exception {
        when(chargeBoxRepository.findByChargeBoxId("CP001")).thenReturn(Optional.of(registeredCharger("CP001", "secret")));

        ServerHttpRequest request = request("/CP001", basicAuthHeader("CP001", "wrong-password"));
        ServerHttpResponse response = response();

        boolean allowed = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

        assertThat(allowed).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handshake_chargeBoxIdMismatch_rejected() throws Exception {
        when(chargeBoxRepository.findByChargeBoxId("CP001")).thenReturn(Optional.of(registeredCharger("CP001", "secret")));

        // Basic Auth username doesn't match the charger id in the URL path.
        ServerHttpRequest request = request("/CP001", basicAuthHeader("SOME-OTHER-CP", "secret"));
        ServerHttpResponse response = response();

        boolean allowed = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

        assertThat(allowed).isFalse();
    }

    @Test
    void handshake_unknownCharger_rejected() throws Exception {
        when(chargeBoxRepository.findByChargeBoxId("CP-GHOST")).thenReturn(Optional.empty());

        ServerHttpRequest request = request("/CP-GHOST", basicAuthHeader("CP-GHOST", "whatever"));
        ServerHttpResponse response = response();

        boolean allowed = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

        assertThat(allowed).isFalse();
    }

    @Test
    void handshake_chargerWithNoPasswordProvisioned_rejected() throws Exception {
        ChargeBox cb = new ChargeBox();
        cb.setChargeBoxId("CP001");
        cb.setPasswordHash(null);
        when(chargeBoxRepository.findByChargeBoxId("CP001")).thenReturn(Optional.of(cb));

        ServerHttpRequest request = request("/CP001", basicAuthHeader("CP001", "anything"));
        ServerHttpResponse response = response();

        boolean allowed = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

        assertThat(allowed).isFalse();
    }

    @Test
    void handshake_validCredentials_upgraded() throws Exception {
        when(chargeBoxRepository.findByChargeBoxId("CP001")).thenReturn(Optional.of(registeredCharger("CP001", "secret")));

        ServerHttpRequest request = request("/CP001", basicAuthHeader("CP001", "secret"));
        ServerHttpResponse response = response();

        boolean allowed = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

        assertThat(allowed).isTrue();
        verify(response, never()).setStatusCode(HttpStatus.UNAUTHORIZED);
    }
}
