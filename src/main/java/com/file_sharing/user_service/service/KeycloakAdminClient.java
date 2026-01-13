package com.file_sharing.user_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class KeycloakAdminClient {

    private final WebClient webClient;
    private final String realm;

    public KeycloakAdminClient(
            @Value("${keycloak.admin-base-url}") String baseUrl,
            @Value("${keycloak.realm}") String realm
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.realm = realm;
    }

    public void deleteUser(String userId, String adminToken) {
        webClient.delete()
                .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .headers(h -> h.setBearerAuth(adminToken))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}