package com.niskora.identity;

import java.time.Clock;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates users, with their password hashed by Argon2 (PLT-17). */
@Service
class Users {

  /** The shortest password a user may have. */
  static final int SHORTEST_PASSWORD = 8;

  private final UserRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  Users(UserRepository repository, PasswordEncoder passwordEncoder, Clock clock) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  /**
   * Creates the first user (PLT-23), and refuses once any user exists: the others are added by a
   * user on the Users screen.
   */
  @Transactional
  UUID createFirstUser(String loginName, String name, String password) {
    if (repository.count() > 0) {
      throw new UserRefusedException("A user already exists, so this is not the first one.");
    }
    return create(loginName, name, password);
  }

  @Transactional
  UUID create(String loginName, String name, String password) {
    String login = loginName.strip();
    String shownName = name.strip();
    if (login.isEmpty() || login.chars().anyMatch(Character::isWhitespace)) {
      throw new UserRefusedException("The login name must be one word.");
    }
    if (shownName.isEmpty()) {
      throw new UserRefusedException("The name must not be blank.");
    }
    if (password.length() < SHORTEST_PASSWORD) {
      throw new UserRefusedException(
          "The password must be at least " + SHORTEST_PASSWORD + " characters long.");
    }
    if (repository.findByLoginName(login).isPresent()) {
      throw new UserRefusedException("The login name " + login + " is taken.");
    }
    User user =
        new User(
            UUID.randomUUID(), login, shownName, passwordEncoder.encode(password), clock.instant());
    return repository.save(user).id();
  }

  /** A user that cannot be created, with the reason why. */
  static final class UserRefusedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    UserRefusedException(String message) {
      super(message);
    }
  }
}
