package com.zyelectric.ocpp.controller;

import com.zyelectric.ocpp.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the REST-layer authentication rules: Swagger/actuator lockdown, login, and JWT
 * enforcement on the admin API. Complements OcppHandshakeInterceptorTest, which covers the
 * separate WebSocket (charger) auth path.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    private static final String TEST_PASSWORD = "test-password-123";
    private static final String TEST_PASSWORD_HASH = new BCryptPasswordEncoder().encode(TEST_PASSWORD);

    @DynamicPropertySource
    static void adminPassword(DynamicPropertyRegistry registry) {
        registry.add("app.admin.password-hash", () -> TEST_PASSWORD_HASH);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void swaggerEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorNonHealthEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorHealthEndpoint_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void login_wrongPassword_rejected() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_correctPassword_returnsValidJwt() throws Exception {
        String token = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("admin");
    }

    @Test
    void login_blankPassword_rejectedByValidation() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoint_rejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/chargers")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_rejectsExpiredToken() throws Exception {
        // A token whose expiration is already in the past.
        JwtUtil expiredIssuer = new JwtUtil("test-only-secret-key-for-junit-do-not-use-in-production-0123456789abcdef", -1000L);
        String expired = expiredIssuer.generateToken("admin");

        mockMvc.perform(get("/api/chargers").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_acceptsValidToken() throws Exception {
        String token = jwtUtil.generateToken("admin");

        mockMvc.perform(get("/api/chargers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
