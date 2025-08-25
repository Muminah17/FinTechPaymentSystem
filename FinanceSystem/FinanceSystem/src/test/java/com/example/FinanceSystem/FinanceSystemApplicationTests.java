package com.example.FinanceSystem;

import com.example.FinanceSystem.dto.CreateAccount;
import com.example.FinanceSystem.dto.TransferRequest;
import com.example.FinanceSystem.dto.TransferResponse;
import com.example.FinanceSystem.exception.ConflictException;
import com.example.FinanceSystem.exception.InsufficientFundsException;
import com.example.FinanceSystem.repository.LedgerEntryRepository;
import com.example.FinanceSystem.service.AccountService;
import com.example.FinanceSystem.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FinanceSystemApplicationTests {


	@Autowired AccountService accountService;
	@Autowired LedgerService ledgerService;
	@Autowired LedgerEntryRepository ledgerRepo;

	@Test
	void happyPathTransfer() {
		var a1 = accountService.create(req(100));
		var a2 = accountService.create(req(10));

		var t = transfer(a1.getId(), a2.getId(), 25);
		TransferResponse res = ledgerService.applyTransfer(t);

		assertEquals("SUCCESS", res.getStatus());
		assertEquals(new BigDecimal("75.0000"), accountService.get(a1.getId()).getBalance());
		assertEquals(new BigDecimal("35.0000"), accountService.get(a2.getId()).getBalance());
		assertEquals(2, ledgerRepo.findByTransferId(t.getTransferId()).size());
	}

	@Test
	void insufficientFundsFails() {
		var a1 = accountService.create(req(10));
		var a2 = accountService.create(req(5));

		var t = transfer(a1.getId(), a2.getId(), 50);

		assertThrows(InsufficientFundsException.class,
				() -> ledgerService.applyTransfer(t));
		// Ensure no ledger entries written
		assertTrue(ledgerRepo.findByTransferId(t.getTransferId()).isEmpty());
	}

	@Test
	void idempotentReplayReturnsSameResult() {
		var a1 = accountService.create(req(100));
		var a2 = accountService.create(req(0));

		var t = transfer(a1.getId(), a2.getId(), 10);

		var r1 = ledgerService.applyTransfer(t);
		var r2 = ledgerService.applyTransfer(t);

		assertEquals(r1.getStatus(), r2.getStatus());
		assertEquals(new BigDecimal("90.0000"), accountService.get(a1.getId()).getBalance());
		assertEquals(new BigDecimal("10.0000"), accountService.get(a2.getId()).getBalance());
	}

	@Test
	void concurrentTransfersAreConsistent() throws Exception {
		var a1 = accountService.create(req(1000));
		var a2 = accountService.create(req(0));

		int n = 20;
		ExecutorService exec = Executors.newFixedThreadPool(8);
		List<Callable<Void>> tasks = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			tasks.add(() -> {
				ledgerService.applyTransfer(transfer(a1.getId(), a2.getId(), 10));
				return null;
			});
		}
		exec.invokeAll(tasks);
		exec.shutdown();
		exec.awaitTermination(10, TimeUnit.SECONDS);

		assertEquals(new BigDecimal("800.0000"), accountService.get(a1.getId()).getBalance());
		assertEquals(new BigDecimal("200.0000"), accountService.get(a2.getId()).getBalance());
	}

	@Test
	void sameAccountTransferFails() {
		var a1 = accountService.create(req(100));

		var t = transfer(a1.getId(), a1.getId(), 10);

		assertThrows(ConflictException.class, () -> ledgerService.applyTransfer(t));
	}

	private CreateAccountRequest req(int balance) {
		var r = new CreateAccountRequest();
		r.setInitialBalance(new BigDecimal(balance));
		return r;
	}

	private TransferRequest transfer(Long from, Long to, int amount) {
		TransferRequest t = new TransferRequest();
		t.setTransferId(UUID.randomUUID().toString());
		t.setFromAccountId(from);
		t.setToAccountId(to);
		t.setAmount(new BigDecimal(amount));
		return t;
	}

}
