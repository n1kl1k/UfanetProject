package com.example.un.repository;

import com.example.un.models.MountlyAccumulative;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface MountlyAccumulativeRepository extends JpaRepository<MountlyAccumulative, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MountlyAccumulative m WHERE m.accountId = :accountId AND m.serviceId = :serviceId AND m.monthYear = :monthYear")
    Optional<MountlyAccumulative> findByAccountIdAndServiceIdAndMonthYear(
            @Param("accountId") Long accountId,
            @Param("serviceId") Long serviceId,
            @Param("monthYear") LocalDate monthYear);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MountlyAccumulative m WHERE m.accountId = :accountId AND m.serviceId = :serviceId AND m.monthYear < :monthYear ORDER BY m.monthYear DESC")
    Optional<MountlyAccumulative> findPreviousByAccountIdAndServiceId(
            @Param("accountId") Long accountId,
            @Param("serviceId") Long serviceId,
            @Param("monthYear") LocalDate monthYear);
}