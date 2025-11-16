package com.file_sharing.user_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NatsEventPublisher {

  private final Connection nats;
  private final ObjectMapper mapper = new ObjectMapper();

  public NatsEventPublisher(Connection nats) {
    this.nats = nats;
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
}
