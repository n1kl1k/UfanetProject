package com.example.un.repository;

import com.example.un.models.Cumulative;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.Optional;


public interface CumulativeRepository extends JpaRepository<Cumulative, Long> {
    @Query("SELECT c.accountId FROM Cumulative c WHERE c.id = :id")
    String getAccountHave(@Param("id") Long id);

    @Query("SELECT c.amount FROM Cumulative c WHERE c.accountId = :accountId ORDER BY c.createAt DESC LIMIT 1")
    Optional<BigDecimal> findLastAmountByAccountId(@Param("accountId") Long accountId);


}
