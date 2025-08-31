package com.example.ledger_service.repository;

import com.example.ledger_service.entity.LedgerEntry;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LedgerEntryRepository extends CrudRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransferId(String transferId);
}
