package com.niskora.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A user: one person with one account, never shared (PLT-19). They log in with their login name,
 * which is unique whatever its case, and the app shows their full name.
 *
 * <p>An account is never deleted, so everything it did keeps its name; it is disabled instead
 * (PLT-45). Each change raises its version (ENG-87) and records who made it and when (FE-81).
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

  /** Raised by Hibernate on every change, which a change's If-Match must name (ENG-87). */
  @Version private long version;

  @Column(name = "is_disabled")
  private boolean disabled;

  /** When the latest change was made: when the account was created, until it is changed. */
  @Column(name = "changed_at")
  private Instant changedAt;

  /** Who made the latest change; null until a user changes the account. */
  @Column(name = "changed_by")
  private @Nullable UUID changedBy;

  User(UUID id, String loginName, String fullName, String passwordHash, Instant createdAt) {
    this.id = id;
    this.loginName = loginName;
    this.fullName = fullName;
    this.passwordHash = passwordHash;
    this.createdAt = createdAt;
    this.changedAt = createdAt;
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

  Instant createdAt() {
    return createdAt;
  }

  long version() {
    return version;
  }

  boolean isDisabled() {
    return disabled;
  }

  Instant changedAt() {
    return changedAt;
  }

  @Nullable UUID changedBy() {
    return changedBy;
  }

  void setPasswordHash(String passwordHash, UUID by, Instant at) {
    this.passwordHash = passwordHash;
    changed(by, at);
  }

  void setDisabled(boolean disabled, UUID by, Instant at) {
    this.disabled = disabled;
    changed(by, at);
  }

  private void changed(UUID by, Instant at) {
    changedBy = by;
    changedAt = at;
  }
}
