package com.rikkeibank.customer;

import com.rikkeibank.customer.service.CustomerService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CacheManager cacheManager;

    @Value("${rikkeibank.jwt.secret}")
    private String secret;

    @Value("${rikkeibank.internal-key}")
    private String internalKey;

    private String token(long uid, String username, String role, Long customerId, Long staffId, String branch) {
        long now = System.currentTimeMillis();
        return "Bearer " + Jwts.builder()
                .subject(username)
                .claim("uid", uid)
                .claim("role", role)
                .claim("customerId", customerId)
                .claim("staffId", staffId)
                .claim("branchCode", branch)
                .claim("type", "access")
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + 15 * 60 * 1000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String admin() {
        return token(1, "admin", "ADMIN", null, 3L, "HN01");
    }

    @Test
    void crudCustomer() throws Exception {
        String body = """
                {"code":"CUS900","fullName":"Khách Thử","idNumber":"001099999900","phone":"0909999900",
                 "email":"thu900@rikkeibank.vn","address":"Hà Nội","branchCode":"HN01"}
                """;
        String res = mockMvc.perform(post("/api/customers").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CUS900"))
                .andReturn().getResponse().getContentAsString();
        String id = res.replaceAll(".*\"id\":(\\d+).*", "$1");

        // trùng mã -> 409
        mockMvc.perform(post("/api/customers").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        String update = body.replace("Khách Thử", "Khách Đã Sửa");
        mockMvc.perform(put("/api/customers/" + id).header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Khách Đã Sửa"));

        mockMvc.perform(delete("/api/customers/" + id).header("Authorization", admin()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/customers/" + id).header("Authorization", admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/customers/" + id));
    }

    @Test
    void invalidIdNumberReturns400() throws Exception {
        String body = """
                {"code":"CUS901","fullName":"Sai CCCD","idNumber":"123","phone":"0909999901",
                 "email":"sai@rikkeibank.vn","branchCode":"HN01"}
                """;
        mockMvc.perform(post("/api/customers").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void readTwiceHitsDatabaseOnce() throws Exception {
        cacheManager.getCache("customers").clear();
        long before = customerService.getDbQueryCount();

        mockMvc.perform(get("/api/customers/2").header("Authorization", admin())).andExpect(status().isOk());
        mockMvc.perform(get("/api/customers/2").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CUS002"));

        assertEquals(before + 1, customerService.getDbQueryCount());
    }

    @Test
    void customerCannotViewOtherCustomer() throws Exception {
        String an = token(4, "an", "CUSTOMER", 1L, null, "HN01");
        mockMvc.perform(get("/api/customers/2").header("Authorization", an))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(get("/api/customers/1").header("Authorization", an))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/customers/me").header("Authorization", an))
                .andExpect(jsonPath("$.code").value("CUS001"));
        // CUSTOMER không được xem danh sách
        mockMvc.perform(get("/api/customers").header("Authorization", an))
                .andExpect(status().isForbidden());
    }

    @Test
    void tellerOtherBranchAndNoToken() throws Exception {
        String tellerHcm = token(3, "teller.hcm", "TELLER", null, 2L, "HCM01");
        mockMvc.perform(get("/api/customers/1").header("Authorization", tellerHcm))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void internalRequiresKey() throws Exception {
        mockMvc.perform(get("/internal/customers/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(get("/internal/customers/1").header("X-Internal-Key", internalKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CUS001"));
    }
}
