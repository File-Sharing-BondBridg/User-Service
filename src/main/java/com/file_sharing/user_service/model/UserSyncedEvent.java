package com.file_sharing.user_service.model;

import java.time.Instant;

public record UserSyncedEvent(
    String eventType,
    String userId,
    String email,
    Instant timestamp
) {}
