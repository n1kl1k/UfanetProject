package com.example.un.repository;

import com.example.un.models.SnapshotBalance;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SnapshotBalanceRepository extends JpaRepository<SnapshotBalance,Long> {
    Optional<SnapshotBalance> findTopByAccountIdAndSnapshotDateLessThanEqualOrderBySnapshotDateDesc(
            Long accountId, LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    boolean existsByAccountIdAndSnapshotDate(Long accountId, LocalDate snapshotDate);

    @Modifying
    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "ALTER TABLE snapshot_balance DROP PARTITION :partitionName", nativeQuery = true)
    void dropPartition(@Param("partitionName") String partitionName);

    @Query(value = "SELECT PARTITION_NAME FROM information_schema.PARTITIONS " +
            "WHERE TABLE_NAME = 'snapshot_balance' AND TABLE_SCHEMA = DATABASE()",
            nativeQuery = true)
    List<String> findAllPartitionNames();
}
