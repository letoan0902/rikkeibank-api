package com.rikkeibank.transaction.saga;

import com.rikkeibank.transaction.entity.EntryType;
import com.rikkeibank.transaction.entity.LedgerEntry;
import com.rikkeibank.transaction.entity.Transaction;
import com.rikkeibank.transaction.repository.LedgerEntryRepository;
import com.rikkeibank.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Mỗi lần đổi trạng thái saga là một giao dịch CSDL riêng, commit ngay
@Service
public class TransactionStateService {

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public TransactionStateService(TransactionRepository transactionRepository,
                                   LedgerEntryRepository ledgerEntryRepository) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction save(Transaction tx) {
        return transactionRepository.save(tx);
    }

    // Lưu trạng thái cuối kèm 2 bút toán trong cùng một giao dịch
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction saveWithLedger(Transaction tx, String account1, EntryType type1, Long balance1,
                                      String account2, EntryType type2, Long balance2) {
        Transaction saved = transactionRepository.save(tx);
        ledgerEntryRepository.save(new LedgerEntry(tx.getTransactionCode(), account1, type1, tx.getAmount(), balance1));
        ledgerEntryRepository.save(new LedgerEntry(tx.getTransactionCode(), account2, type2, tx.getAmount(), balance2));
        return saved;
    }
}
