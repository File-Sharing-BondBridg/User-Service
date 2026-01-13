package com.file_sharing.user_service.controller;

import com.file_sharing.user_service.model.User;
import com.file_sharing.user_service.model.UserSyncedEvent;
import com.file_sharing.user_service.repository.UserRepository;
import com.file_sharing.user_service.service.KeycloakAdminClient;
import com.file_sharing.user_service.service.KeycloakTokenService;
import com.file_sharing.user_service.service.NatsEventPublisher;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
public class UserController {

    private final UserRepository repo;
    private final NatsEventPublisher natsPublisher;
    private final KeycloakAdminClient keycloakAdminClient;
    private final KeycloakTokenService keycloakTokenService;

    public UserController(UserRepository repo, NatsEventPublisher natsPublisher, KeycloakAdminClient keycloakAdminClient, KeycloakTokenService keycloakTokenService) {
        this.repo = repo;
        this.natsPublisher = natsPublisher;
        this.keycloakAdminClient = keycloakAdminClient;
        this.keycloakTokenService = keycloakTokenService;
    }

    @PostMapping
    public ResponseEntity<User> createOrGetUser(@RequestBody Map<String, String> data) {
        String email = data.get("email");
        String name = data.get("name");

        return ResponseEntity.ok(
            repo.findByEmail(email)
                .orElseGet(
                    () -> {
                      User user = new User(email, name, "keycloak");
                      throw new IllegalStateException("Cannot create user without JWT");
                    }
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String email = jwt.getClaim("email");
        return ResponseEntity.ok(Map.of("id", userId, "email", email));
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncUser( @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String email = jwt.getClaim("email");
        String name = jwt.getClaim("preferred_username");

        if (email == null) {
          return ResponseEntity.badRequest().body(Map.of("error", "Email missing in token"));
        }

        User user =
            repo.findByEmail(email)
                .orElseGet(
                    () -> {
                      User newUser = new User(email, name, "keycloak");
                      newUser.setId(userId);
                      return repo.save(newUser);
                    });

        // If user exists but id is missing (old data), fix it
        if (user.getId() == null || !user.getId().equals(userId)) {
          user.setId(userId);
          user = repo.save(user);
        }

        UserSyncedEvent event =
              new UserSyncedEvent(
                      "UserSynced",
                      user.getId(),
                      user.getEmail(),
                      Instant.now()
              );

        natsPublisher.publish("users.synced", event);

        return ResponseEntity.ok(
            Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "provider", user.getProvider()
            )
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyAccount(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();

        if (!repo.existsById(userId)) {
          return ResponseEntity.notFound().build();
        }

        repo.deleteById(userId);
        natsPublisher.publishUserDeleted(userId);

        try {
            String adminToken = keycloakTokenService.getAdminToken();
            keycloakAdminClient.deleteUser(userId, adminToken);
        } catch (Exception e) {
            System.err.println("Failed to delete user from Keycloak: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of("message", "Account deleted successfully", "user_id", userId));
    }
}
