package com.niskora.identity;

import java.time.Clock;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates users, with their password hashed by Argon2 (PLT-17). */
@Service
class Users {

  /** The shortest password a user may have. */
  static final int SHORTEST_PASSWORD = 8;

  /** The unique index that keeps two users from sharing a login name, whatever its case. */
  private static final String LOGIN_NAME_KEY = "users_login_name_key";

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
   * user on the Users screen. Two run at the same moment create one user, not two.
   */
  @Transactional
  UUID createFirstUser(String loginName, String fullName, String password) {
    repository.lockAgainstNewUsers();
    if (repository.count() > 0) {
      throw new UserRefusedException("A user already exists, so this is not the first one.");
    }
    return create(loginName, fullName, password);
  }

  @Transactional
  UUID create(String loginName, String fullName, String password) {
    String login = loginName.strip();
    String name = fullName.strip();
    if (login.isEmpty() || login.chars().anyMatch(Character::isWhitespace)) {
      throw new UserRefusedException("The login name must be one word.");
    }
    if (name.isEmpty()) {
      throw new UserRefusedException("The full name must not be blank.");
    }
    if (password.length() < SHORTEST_PASSWORD) {
      throw new UserRefusedException(
          "The password must be at least " + SHORTEST_PASSWORD + " characters long.");
    }
    if (repository.findByLoginName(login).isPresent()) {
      throw loginNameTaken(login);
    }
    User user =
        new User(UUID.randomUUID(), login, name, passwordEncoder.encode(password), clock.instant());
    try {
      return repository.saveAndFlush(user).id();
    } catch (DataIntegrityViolationException failed) {
      // Another user took the login name since the check above.
      if (failed.getCause() instanceof ConstraintViolationException violation
          && LOGIN_NAME_KEY.equals(violation.getConstraintName())) {
        throw loginNameTaken(login);
      }
      throw failed;
    }
  }

  private static UserRefusedException loginNameTaken(String loginName) {
    return new UserRefusedException("The login name " + loginName + " is taken.");
  }

  /** A user that cannot be created, with the reason why. */
  static final class UserRefusedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    UserRefusedException(String message) {
      super(message);
    }
  }
}
