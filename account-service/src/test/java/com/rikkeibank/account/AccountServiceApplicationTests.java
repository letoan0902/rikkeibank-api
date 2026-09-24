package com.rikkeibank.account;

import com.rikkeibank.account.repository.AccountRepository;
import com.rikkeibank.account.repository.BalanceOperationRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountServiceApplicationTests {

    private static final String KEY = "rikkeibank-internal-key-2026";
    private static final String SECRET = "RikkeiBankApiSecretKeyDungChungChoGatewayVaCacMicroservice2026!!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BalanceOperationRepository operationRepository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    private long balanceOf(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).orElseThrow().getBalance();
    }

    private String newCode() {
        return "TX" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private ResultActions call(String op, String code, String accountNumber, long amount) throws Exception {
        String body = "{\"transactionCode\":\"" + code + "\",\"accountNumber\":\"" + accountNumber
                + "\",\"amount\":" + amount + "}";
        return mockMvc.perform(post("/internal/accounts/" + op)
                .header("X-Internal-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void debitThanhCongGiamSoDu() throws Exception {
        long before = balanceOf("100000000004");
        String code = newCode();
        call("debit", code, "100000000004", 1_000_000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfter").value(before - 1_000_000))
                .andExpect(jsonPath("$.alreadyProcessed").value(false))
                .andExpect(jsonPath("$.servedBy").value("account-service:8083"));
        assertThat(balanceOf("100000000004")).isEqualTo(before - 1_000_000);
        verify(kafkaTemplate, times(1)).send(eq("account-events"), eq(code), anyString());
    }

    @Test
    void debitTrungMaGiaoDichKhongTruLanHai() throws Exception {
        long before = balanceOf("100000000004");
        String code = newCode();
        call("debit", code, "100000000004", 500_000).andExpect(status().isOk());
        call("debit", code, "100000000004", 500_000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyProcessed").value(true))
                .andExpect(jsonPath("$.balanceAfter").value(before - 500_000));
        assertThat(balanceOf("100000000004")).isEqualTo(before - 500_000);
        assertThat(operationRepository.countByTransactionCode(code)).isEqualTo(1);
    }

    @Test
    void creditVaoTaiKhoanFrozenTra409() throws Exception {
        long before = balanceOf("100000000003");
        String code = newCode();
        call("credit", code, "100000000003", 100_000)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("ACCOUNT_NOT_ACTIVE")));
        assertThat(balanceOf("100000000003")).isEqualTo(before);
        assertThat(operationRepository.countByTransactionCode(code)).isZero();
    }

    @Test
    void debitQuaSoDuTra409() throws Exception {
        long before = balanceOf("100000000002");
        String code = newCode();
        call("debit", code, "100000000002", before + 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("INSUFFICIENT_BALANCE")));
        assertThat(balanceOf("100000000002")).isEqualTo(before);
        assertThat(operationRepository.countByTransactionCode(code)).isZero();
    }

    @Test
    void refundCongLaiDung() throws Exception {
        long before = balanceOf("100000000001");
        String code = newCode();
        call("debit", code, "100000000001", 2_000_000).andExpect(status().isOk());
        assertThat(balanceOf("100000000001")).isEqualTo(before - 2_000_000);
        call("refund", code, "100000000001", 2_000_000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("REFUND"))
                .andExpect(jsonPath("$.balanceAfter").value(before));
        assertThat(balanceOf("100000000001")).isEqualTo(before);
    }

    @Test
    void internalThieuKeyTra403() throws Exception {
        mockMvc.perform(get("/internal/accounts/100000000001"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/internal/accounts/100000000001"));
    }

    @Test
    void customerXemTaiKhoanNguoiKhacTra403() throws Exception {
        String token = customerToken(5L, "binh", 2L, "HCM01");
        mockMvc.perform(get("/api/accounts/100000000001").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(get("/api/accounts/100000000002").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(2));
    }

    private String customerToken(Long uid, String username, Long customerId, String branchCode) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("uid", uid)
                .claim("role", "CUSTOMER")
                .claim("customerId", customerId)
                .claim("branchCode", branchCode)
                .claim("type", "access")
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 15 * 60 * 1000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
