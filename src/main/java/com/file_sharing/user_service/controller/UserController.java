package com.file_sharing.user_service.controller;

import com.file_sharing.user_service.service.NatsEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.file_sharing.user_service.repository.UserRepository;
import com.file_sharing.user_service.model.User;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository repo;
    private final NatsEventPublisher natsPublisher;

    public UserController(UserRepository repo, NatsEventPublisher natsPublisher) {
        this.repo = repo;
        this.natsPublisher = natsPublisher;
    }

    @PostMapping
    public ResponseEntity<User> createOrGetUser(@RequestBody Map<String, String> data) {
        String email = data.get("email");
        String name = data.get("name");
        // You probably have JWT here too — but if not, you can't set ID

        return ResponseEntity.ok(
                repo.findByEmail(email)
                        .orElseGet(() -> {
                            User user = new User(email, name, "keycloak");
                            // You MUST have JWT to set ID → better remove this endpoint
                            // or require JWT
                            throw new IllegalStateException("Cannot create user without JWT");
                        })
        );
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String email = jwt.getClaim("email");
        return ResponseEntity.ok(Map.of("id", userId, "email", email));
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();  // ← Keycloak UUID
        String email = jwt.getClaim("email");
        String name = jwt.getClaim("preferred_username");

        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email missing in token"));
        }

        User user = repo.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User(email, name, "keycloak");
                    newUser.setId(userId);  // ← SET ID MANUALLY
                    return repo.save(newUser);
                });

        // If user exists but id is missing (old data), fix it
        if (user.getId() == null || !user.getId().equals(userId)) {
            user.setId(userId);
            user = repo.save(user);
        }

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "provider", user.getProvider()
        ));
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyAccount(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject(); // e.g. "7f010923-5f70-..."

        // todo: first create a user that has a string id. Currently the IDs dont match
        if (!repo.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }

        repo.deleteById(userId); // ← Works with String ID

        natsPublisher.publishUserDeleted(userId);

        return ResponseEntity.ok(Map.of(
                "message", "Account deleted successfully",
                "user_id", userId
        ));
    }
}