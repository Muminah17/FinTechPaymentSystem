package com.example.FinanceSystem.repository;

import com.example.FinanceSystem.entity.Account;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<Account, Long> {

}
