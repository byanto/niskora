package com.niskora.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A user: one person with one account, never shared (PLT-19). They log in with their login name,
 * which is unique whatever its case, and the app shows their full name.
 */
@Entity
@Table(schema = "identity", name = "users")
class User {

  @Id private UUID id;

  @Column(name = "login_name")
  private String loginName;

  @Column(name = "full_name")
  private String fullName;

  /** The Argon2 hash of the password, prefixed with the encoder's id (PLT-17). */
  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "created_at")
  private Instant createdAt;

  User(UUID id, String loginName, String fullName, String passwordHash, Instant createdAt) {
    this.id = id;
    this.loginName = loginName;
    this.fullName = fullName;
    this.passwordHash = passwordHash;
    this.createdAt = createdAt;
  }

  /** For Hibernate, which sets the fields itself. */
  @SuppressWarnings("NullAway.Init")
  protected User() {}

  UUID id() {
    return id;
  }

  String loginName() {
    return loginName;
  }

  String fullName() {
    return fullName;
  }

  String passwordHash() {
    return passwordHash;
  }
}
