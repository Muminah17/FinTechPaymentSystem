package com.example.transfer_service.repository;

import com.example.transfer_service.entity.TransferIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<TransferIdempotency, String> {
}
