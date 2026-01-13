package com.file_sharing.user_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class KeycloakTokenService {

    private final WebClient webClient;

    @Value("${keycloak.token-url}")
    private String tokenUrl;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    public KeycloakTokenService() {
        this.webClient = WebClient.builder().build();
    }

    public String getAdminToken() {
        Map<String, Object> response =
                webClient.post()
                        .uri(tokenUrl)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(
                                BodyInserters
                                        .fromFormData("grant_type", "client_credentials")
                                        .with("client_id", clientId)
                                        .with("client_secret", clientSecret)
                        )
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();

        assert response != null;
        return (String) response.get("access_token");
    }
}

