package com.example.un;

import com.example.un.repository.SnapshotBalanceRepository;
import com.example.un.services.ArchiveSnapshotService;
import com.example.un.services.PartitionService;
import com.example.un.services.SnapshotBalanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = "balance.snapshot.retention-months=3")
class ScheduledServicesIntegrationTest {

    @Autowired
    PartitionService partitionService;
    @Autowired
    SnapshotBalanceService snapshotBalanceService;
    @Autowired
    ArchiveSnapshotService archiveSnapshotService;
    @Autowired
    SnapshotBalanceRepository sBRepo;

    @Test
    void partitionService_addsPartition() {
        assertDoesNotThrow(() -> partitionService.addNextMonthPartition());
    }

    @Test
    void snapshotService_createsSnapshots() {
        snapshotBalanceService.createMountSnapshot();
        LocalDate lastDay = LocalDate.now().withDayOfMonth(1).minusDays(1);
        assertTrue(sBRepo.count() > 0);
    }

    @Test
    void archiveService_dropsOldPartitions() {
        assertDoesNotThrow(() -> archiveSnapshotService.deleteOldPartitions());
    }
}
