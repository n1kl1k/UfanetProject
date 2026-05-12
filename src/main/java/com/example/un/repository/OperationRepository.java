package com.example.un.repository;

import com.example.un.models.Operation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface OperationRepository extends JpaRepository<Operation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Operation> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "SELECT COALESCE(SUM(CASE WHEN operation_type = 'PAYMENT' THEN amount ELSE -amount END), 0) " +
            "FROM operation " +
            "WHERE account_id = :accountId AND created_at BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    BigDecimal sumAmountByAccountAndDateRange(@Param("accountId") Long accountId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
}
