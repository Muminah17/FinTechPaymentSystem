package com.example.FinanceSystem.repository;

import com.example.FinanceSystem.entity.LedgerEntry;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LedgerEntryRepository extends CrudRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransferId(String transferId);
}
