package com.example.un.repository;

import com.example.un.models.Operation;
import com.example.un.models.Reference;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ReferenceRepository extends JpaRepository<Reference, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    boolean existsByReferenceId(String referenceId);
}
