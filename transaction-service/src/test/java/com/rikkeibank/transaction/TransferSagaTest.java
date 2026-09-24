package com.rikkeibank.transaction;

import com.rikkeibank.transaction.client.AccountClient;
import com.rikkeibank.transaction.dto.AccountInfo;
import com.rikkeibank.transaction.dto.BalanceOperationRequest;
import com.rikkeibank.transaction.dto.BalanceOperationResponse;
import com.rikkeibank.transaction.dto.TransferRequest;
import com.rikkeibank.transaction.entity.EntryType;
import com.rikkeibank.transaction.entity.LedgerEntry;
import com.rikkeibank.transaction.entity.Transaction;
import com.rikkeibank.transaction.entity.TransactionStatus;
import com.rikkeibank.transaction.exception.BusinessException;
import com.rikkeibank.transaction.repository.LedgerEntryRepository;
import com.rikkeibank.transaction.repository.TransactionRepository;
import com.rikkeibank.transaction.saga.TransferSagaOrchestrator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferSagaTest {

    private static final String FROM = "100000000001";
    private static final String TO = "100000000002";

    @Autowired
    private TransferSagaOrchestrator orchestrator;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountClient accountClient;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${rikkeibank.jwt.secret}")
    private String secret;

    @BeforeEach
    void setUp() {
        reset(accountClient);
        when(accountClient.getAccount(FROM)).thenReturn(new AccountInfo(FROM, 1L, 50_000_000L, "ACTIVE", "HN01", "test"));
        when(accountClient.getAccount(TO)).thenReturn(new AccountInfo(TO, 2L, 20_000_000L, "ACTIVE", "HCM01", "test"));
        when(accountClient.debit(any())).thenAnswer(inv -> ok(inv.getArgument(0), "DEBIT", 49_000_000L));
        when(accountClient.credit(any())).thenAnswer(inv -> ok(inv.getArgument(0), "CREDIT", 21_000_000L));
        when(accountClient.refund(any())).thenAnswer(inv -> ok(inv.getArgument(0), "REFUND", 50_000_000L));
    }

    @Test
    void chuyenKhoanThanhCong_completedVaCo2ButToan() {
        Transaction tx = orchestrator.transfer(1L, new TransferRequest(FROM, TO, 1_000_000L, "test", false));

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(tx.getBranchCode()).isEqualTo("HN01");
        assertThat(tx.getToCustomerId()).isEqualTo(2L);
        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionCode(tx.getTransactionCode());
        assertThat(entries).hasSize(2);
        assertThat(entries).extracting(LedgerEntry::getEntryType)
                .containsExactlyInAnyOrder(EntryType.DEBIT, EntryType.CREDIT);
        verify(accountClient, never()).refund(any());
    }

    @Test
    void creditLoi409_goiRefundVaCompensated() {
        doThrow(new BusinessException(HttpStatus.CONFLICT, "ACCOUNT_NOT_ACTIVE", "Tài khoản đích bị khóa"))
                .when(accountClient).credit(any());

        Transaction tx = orchestrator.transfer(1L, new TransferRequest(FROM, TO, 1_000_000L, "test", false));

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.COMPENSATED);
        verify(accountClient).refund(argThatAccount(FROM));
        Transaction saved = transactionRepository.findByTransactionCode(tx.getTransactionCode()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.COMPENSATED);
    }

    @Test
    void simulateFailure_compensated() {
        Transaction tx = orchestrator.transfer(1L, new TransferRequest(FROM, TO, 500_000L, "test", true));

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.COMPENSATED);
        verify(accountClient, never()).credit(any());
        verify(accountClient).refund(any());
    }

    @Test
    void debitLoi_failedVaKhongGoiRefund() {
        doThrow(new BusinessException(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE", "Số dư không đủ"))
                .when(accountClient).debit(any());

        Transaction tx = orchestrator.transfer(1L, new TransferRequest(FROM, TO, 999_000_000L, "test", false));

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FAILED);
        verify(accountClient, never()).credit(any());
        verify(accountClient, never()).refund(any());
    }

    @Test
    void taiKhoanNguonKhongPhaiCuaMinh_403() throws Exception {
        // khách 2 cố chuyển từ tài khoản của khách 1
        String body = "{\"fromAccountNumber\":\"" + FROM + "\",\"toAccountNumber\":\"" + TO + "\",\"amount\":1000}";
        mockMvc.perform(post("/api/transactions/transfers")
                        .header("Authorization", "Bearer " + token("CUSTOMER", 2L, null, "HCM01"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        verify(accountClient, never()).debit(any());
    }

    @Test
    void tellerReviewGiaoDichKhongDuocPhanCong_403() throws Exception {
        Transaction tx = new Transaction();
        tx.setTransactionCode("TXREVIEW001");
        tx.setFromAccountNumber(FROM);
        tx.setToAccountNumber(TO);
        tx.setAmount(1000L);
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setBranchCode("HN01");
        tx.setAssignedStaffId(1L);
        transactionRepository.save(tx);

        // teller.hcm có staffId=2, giao dịch phân công cho staffId=1
        mockMvc.perform(put("/api/transactions/TXREVIEW001/review")
                        .header("Authorization", "Bearer " + token("TELLER", null, 2L, "HCM01"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"da kiem tra\"}"))
                .andExpect(status().isForbidden());
    }

    private BalanceOperationResponse ok(BalanceOperationRequest r, String type, Long balanceAfter) {
        return new BalanceOperationResponse(r.transactionCode(), r.accountNumber(), type, r.amount(), balanceAfter, false, "test");
    }

    private BalanceOperationRequest argThatAccount(String account) {
        return org.mockito.ArgumentMatchers.argThat(r -> r != null && account.equals(r.accountNumber()));
    }

    private String token(String role, Long customerId, Long staffId, String branchCode) {
        var builder = Jwts.builder()
                .subject("user-test")
                .claim("uid", 99L)
                .claim("role", role)
                .claim("branchCode", branchCode)
                .claim("type", "access")
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000));
        if (customerId != null) {
            builder.claim("customerId", customerId);
        }
        if (staffId != null) {
            builder.claim("staffId", staffId);
        }
        return builder.signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
