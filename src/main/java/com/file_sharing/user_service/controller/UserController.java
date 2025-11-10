package com.file_sharing.user_service.controller;

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
                        new User(null, data.get("email"), data.get("name"), "keycloak")
                ));
    }
}