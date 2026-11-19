package com.niskora.identity;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The user a session belongs to, as Spring Security keeps it in the session. The password hash is
 * there only while the login is checked, and is erased once it has been.
 */
final class LoggedInUser implements UserDetails, CredentialsContainer {

  @Serial private static final long serialVersionUID = 1L;

  private final UUID id;
  private final String loginName;
  private final String name;
  private @Nullable String passwordHash;

  LoggedInUser(UUID id, String loginName, String name, @Nullable String passwordHash) {
    this.id = id;
    this.loginName = loginName;
    this.name = name;
    this.passwordHash = passwordHash;
  }

  static LoggedInUser of(User user) {
    return new LoggedInUser(user.id(), user.loginName(), user.name(), user.passwordHash());
  }

  UUID id() {
    return id;
  }

  String loginName() {
    return loginName;
  }

  String name() {
    return name;
  }

  @Override
  public String getUsername() {
    return loginName;
  }

  @Override
  public @Nullable String getPassword() {
    return passwordHash;
  }

  /** There are no roles in v1 (PLT-20). */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public void eraseCredentials() {
    passwordHash = null;
  }
}
