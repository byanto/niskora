package com.niskora.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface UserRepository extends JpaRepository<User, UUID> {

  /** The user with this login name whatever its case, as the unique index reads it. */
  @Query("SELECT u FROM User u WHERE lower(u.loginName) = lower(:loginName)")
  Optional<User> findByLoginName(String loginName);
}
