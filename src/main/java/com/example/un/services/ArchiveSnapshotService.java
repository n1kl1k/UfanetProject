package com.example.un.services;

import com.example.un.repository.SnapshotBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveSnapshotService {

    private static final DateTimeFormatter PARTITION_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final SnapshotBalanceRepository snapshotRepository;

    @Value("${balance.snapshot.retention-months:3}")
    private int retentionMonths;

    @PostConstruct
    void validateConfig() {
        if (retentionMonths < 1) {
            throw new IllegalStateException(
                    "balance.snapshot.retention-months must be >= 1, got: " + retentionMonths);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOldPartitions() {
        YearMonth thresholdMonth = YearMonth.now().minusMonths(retentionMonths);
        List<String> partitions = snapshotRepository.findAllPartitionNames();

        int dropped = 0;
        int failed = 0;

        for (String partitionName : partitions) {
            if (!partitionName.matches("p\\d{6}")) {
                continue;
            }

            YearMonth partitionMonth = parsePartitionMonth(partitionName);
            if (partitionMonth == null) {
                log.warn("Cannot parse month from partition name: {}", partitionName);
                continue;
            }
            if (partitionMonth.isBefore(thresholdMonth)) {
                try {
                    snapshotRepository.dropPartition(partitionName);
                    log.info("Dropped partition {}", partitionName);
                    dropped++;
                } catch (Exception e) {
                    log.warn("Failed to drop partition {}: {}", partitionName, e.getMessage());
                    failed++;
                }
            }
        }

        log.info("Archive job finished: dropped={}, failed={}, threshold={}",
                dropped, failed, thresholdMonth);
    }

    private YearMonth parsePartitionMonth(String partitionName) {
        try {
            return YearMonth.parse(partitionName.substring(1), PARTITION_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}