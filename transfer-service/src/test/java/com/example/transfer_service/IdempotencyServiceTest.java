package com.example.transfer_service;

import com.example.transfer_service.entity.TransferIdempotency;
import com.example.transfer_service.repository.IdempotencyRecordRepository;
import com.example.transfer_service.service.IdempotencyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdempotencyServiceTest {

    private IdempotencyRecordRepository repo;
    private ObjectMapper mapper;
    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        repo = mock(IdempotencyRecordRepository.class);
        mapper = mock(ObjectMapper.class);
        service = new IdempotencyService(repo, mapper);
    }

    @Test
    void findResponse_returnsResponse_whenRequestHashMatches() {
        String key = "key";
        String requestJson = "{\"foo\":\"bar\"}";
        String responseBody = "{\"result\":\"ok\"}";
        Object request = new Object();

        TransferIdempotency record = new TransferIdempotency();
        record.setRequestHash(requestJson);
        record.setResponseBody(responseBody);

        when(repo.findById(key)).thenReturn(Optional.of(record));
        // Simulate .equals() match
        assertTrue(service.findResponse(key, requestJson).isPresent());
        assertEquals(responseBody, service.findResponse(key, requestJson).get());
    }

    @Test
    void findResponse_returnsEmpty_whenRequestHashDoesNotMatch() {
        String key = "key";
        String requestJson = "{\"foo\":\"bar\"}";
        Object request = new Object();

        TransferIdempotency record = new TransferIdempotency();
        record.setRequestHash("different");
        record.setResponseBody("irrelevant");

        when(repo.findById(key)).thenReturn(Optional.of(record));
        assertTrue(service.findResponse(key, requestJson).isEmpty());
    }

    @Test
    void findResponse_returnsEmpty_whenNoRecord() {
        when(repo.findById("key")).thenReturn(Optional.empty());
        assertTrue(service.findResponse("key", "request").isEmpty());
    }

    @Test
    void stillAlive_returnsTrue_whenNotExpired() {
        String key = "key";
        TransferIdempotency record = new TransferIdempotency();
        record.setExpiresAt(Instant.now().plusSeconds(60));
        when(repo.findById(key)).thenReturn(Optional.of(record));
        assertTrue(service.stillAlive(key));
    }

    @Test
    void stillAlive_returnsFalse_whenExpired() {
        String key = "key";
        TransferIdempotency record = new TransferIdempotency();
        record.setExpiresAt(Instant.now().minusSeconds(60));
        when(repo.findById(key)).thenReturn(Optional.of(record));
        assertFalse(record.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void saveResponse_savesRecord() throws JsonProcessingException {
        String key = "key";
        Object request = "req";
        Object response = "resp";
        when(mapper.writeValueAsString(request)).thenReturn("requestJson");
        when(mapper.writeValueAsString(response)).thenReturn("responseJson");

        // No exception should be thrown
        assertDoesNotThrow(() -> service.saveResponse(key, request, response));
        verify(repo, times(1)).save(any(TransferIdempotency.class));
    }
}
