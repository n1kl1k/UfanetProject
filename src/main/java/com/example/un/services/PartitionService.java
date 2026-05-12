package com.example.un.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartitionService {

    private static final DateTimeFormatter PARTITION_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @PersistenceContext
    private EntityManager entityManager;

    @Scheduled(cron = "0 1 0 1 * *")
    @Transactional
    public void addNextMonthPartition() {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        String partitionName = "p" + nextMonth.format(PARTITION_FORMATTER);
        String boundary = nextMonth.plusMonths(1).atDay(1).toString();

        String sql = String.format(
                "ALTER TABLE snapshot_balance REORGANIZE PARTITION p_future INTO (" +
                        "PARTITION %s VALUES LESS THAN ('%s'), " +
                        "PARTITION p_future VALUES LESS THAN MAXVALUE)",
                partitionName, boundary
        );

        try {
            entityManager.createNativeQuery(sql).executeUpdate();
            log.info("Added partition {} with boundary {}", partitionName, boundary);
        } catch (Exception e) {
            log.error("Failed to add partition {}: {}", partitionName, e.getMessage(), e);
            throw e;
        }
    }
}