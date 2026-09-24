package com.rikkeibank.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.identity.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentityServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userRepository;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);

    @BeforeEach
    void setUp() {
        reset(valueOps);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private JsonNode login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private int refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void loginDungTraVeToken() throws Exception {
        JsonNode res = login("admin", "Admin@123");
        assertThat(res.get("accessToken").asText()).isNotBlank();
        assertThat(res.get("refreshToken").asText()).isNotBlank();
        assertThat(res.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(res.get("role").asText()).isEqualTo("ADMIN");
        assertThat(res.has("password")).isFalse();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + res.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void loginSaiMatKhauTra401() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"sai\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void refreshXoayVongLamTokenCuVoHieu() throws Exception {
        String rt1 = login("cuong", "Customer@123").get("refreshToken").asText();

        String body = mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rt1 + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String rt2 = objectMapper.readTree(body).get("refreshToken").asText();
        assertThat(rt2).isNotEqualTo(rt1);

        assertThat(refresh(rt1)).isEqualTo(401);
        assertThat(refresh(rt2)).isEqualTo(200);
    }

    @Test
    void forceLogoutGhiRedisVaThuHoiRefreshToken() throws Exception {
        String binhRt = login("binh", "Customer@123").get("refreshToken").asText();
        String adminToken = login("admin", "Admin@123").get("accessToken").asText();
        Long binhId = userRepository.findByUsername("binh").orElseThrow().getId();

        mockMvc.perform(post("/api/users/" + binhId + "/force-logout").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revokedAt").isNumber());

        verify(valueOps).set(eq("rb:revoked:" + binhId), anyString(), eq(Duration.ofDays(1)));
        assertThat(refresh(binhRt)).isEqualTo(401);
    }

    @Test
    void userThuongGoiApiUsersBi403() throws Exception {
        String token = login("an", "Customer@123").get("accessToken").asText();
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }
}
