package com.file_sharing.user_service.controller;

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

    public UserController(UserRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public User createOrGetUser(@RequestBody Map<String, String> data) {
        return repo.findByEmail(data.get("email"))
                .orElseGet(() -> repo.save(
                        new User(data.get("email"), data.get("name"), "keycloak")
                ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String email = jwt.getClaim("email");
        return ResponseEntity.ok(Map.of("id", userId, "email", email));
    }

    //todo: implement the functionality
    @PostMapping("/sync")
    public User syncUser(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaim("email");
        String name = jwt.getClaim("preferred_username");

        return repo.findByEmail(email)
                .orElseGet(() -> repo.save(new User(email, name, "keycloak")));
    }
}