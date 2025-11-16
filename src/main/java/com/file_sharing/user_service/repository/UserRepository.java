package com.file_sharing.user_service.repository;

import com.file_sharing.user_service.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
  Optional<User> findByEmail(String email);

  void deleteById(String id);
}
