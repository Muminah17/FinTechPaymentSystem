package com.example.transfer_service.service;

import com.example.transfer_service.dto.TransferResponse;
import com.example.transfer_service.entity.TransferIdempotency;
import com.example.transfer_service.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repo;
    private final ObjectMapper mapper;

    public IdempotencyService(IdempotencyRecordRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public Optional<String> findResponse(String key, Object request) {
        return repo.findById(key)
                .filter(r -> r.getRequestHash().equals(request))
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

/*    private String hash(Object obj) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(mapper.writeValueAsBytes(obj));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }*/
}

