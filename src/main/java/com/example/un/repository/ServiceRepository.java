package com.example.un.repository;

import com.example.un.models.ServiceInCompany;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ServiceRepository extends JpaRepository<ServiceInCompany,Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s.serviceCost FROM ServiceInCompany s WHERE s.id = :id")
    BigDecimal getServiceCostById(@Param("id") Long id);
}
