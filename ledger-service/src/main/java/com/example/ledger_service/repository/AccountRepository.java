package com.example.ledger_service.repository;

import com.example.ledger_service.entity.Account;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<Account, Long> {

}
