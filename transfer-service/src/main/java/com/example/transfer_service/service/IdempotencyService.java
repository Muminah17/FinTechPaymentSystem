package com.example.transfer_service.service;

import com.example.transfer_service.entity.TransferIdempotency;
import com.example.transfer_service.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repo;
    private final ObjectMapper mapper;
    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    public IdempotencyService(IdempotencyRecordRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public Optional<String> findResponse(String key, Object request) {
        return repo.findById(key)
                .filter(r -> r.getRequestHash().equals(writeJson(request)))
                .map(TransferIdempotency::getResponseBody);

    }

    public boolean stillAlive(String key) {
         return repo.findById(key).get().getExpiresAt().isAfter( Instant.now());
    }

    public void saveResponse(String key, Object request, Object response) {
        TransferIdempotency rec = new TransferIdempotency();
        rec.setIdempotencyKey(key);
        rec.setRequestHash(writeJson(request));
        rec.setResponseBody(writeJson(response));
        rec.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        repo.save(rec);
    }

    private String writeJson(Object obj) {
        try { return mapper.writeValueAsString(obj); }
        catch (Exception e) { throw new RuntimeException("Serialization failed", e); }
    }

}

