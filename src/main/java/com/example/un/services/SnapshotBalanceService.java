package com.example.un.services;

import com.example.un.models.Account;
import com.example.un.models.SnapshotBalance;
import com.example.un.repository.AccountRepository;
import com.example.un.repository.SnapshotBalanceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotBalanceService {

    private final AccountRepository accRepo;
    private final SnapshotBalanceRepository sBRepo;
    private final TransactionService trService;

    @Scheduled(cron = "0 5 0 1 * *")
    @Transactional
    public void createMountSnapshot() {
        // Надёжный способ получить последний день предыдущего месяца
        // независимо от того, когда именно запустился крон
        LocalDate lastDayOfPrevMonth = LocalDate.now()
                .withDayOfMonth(1)
                .minusDays(1);

        List<Account> accounts = accRepo.findAll();
        int created = 0;
        int skipped = 0;

        for (Account account : accounts) {
            try {
                if (sBRepo.existsByAccountIdAndSnapshotDate(account.getId(), lastDayOfPrevMonth)) {
                    skipped++;
                    continue;
                }
                BigDecimal balance = trService.getBalanceOnDate(account.getId(), lastDayOfPrevMonth);
                SnapshotBalance snapshot = SnapshotBalance.builder()
                        .account(account)
                        .snapshotDate(lastDayOfPrevMonth)
                        .balance(balance)
                        .build();
                sBRepo.save(snapshot);
                created++;
            } catch (Exception e) {
                log.error("Failed to create snapshot for account {}: {}", account.getId(), e.getMessage(), e);
            }
        }

        log.info("Snapshot job finished for date {}: created={}, skipped={}, total={}",
                lastDayOfPrevMonth, created, skipped, accounts.size());
    }
}