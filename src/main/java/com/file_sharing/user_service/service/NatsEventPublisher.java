package com.file_sharing.user_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import io.nats.client.JetStream;
import org.springframework.stereotype.Service;

@Service
public class NatsEventPublisher {

    private final Connection nats;
    private final ObjectMapper mapper = new ObjectMapper();
    private final JetStream jetStream;

    public NatsEventPublisher(Connection nats) throws IOException {
        this.nats = nats;
        this.jetStream = nats.jetStream();
    }

    public void publishUserDeleted(String userId) {
    Map<String, String> payload = Map.of("user_id", userId);
    try {
      String json = mapper.writeValueAsString(payload);
      nats.publish("user.deleted", json.getBytes(StandardCharsets.UTF_8));
      System.out.println("[NATS] Published user.deleted: " + userId);
    } catch (JsonProcessingException e) {
      System.err.println("[NATS] Failed to serialize user.deleted event: " + e.getMessage());
    }
    }

    public void publish(String subject, Object event) {
        try {
            byte[] payload = mapper.writeValueAsBytes(event);
            jetStream.publish(subject, payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish NATS event", e);
        }
    }
}
