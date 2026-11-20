package com.niskora.identity;

import com.niskora.common.FieldProblem;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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

  private static final String LOGIN_NAME = "loginName";

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

  /**
   * Creates a user, or refuses with every rule the new user breaks at once: a login name that is
   * not one word or is taken whatever its case, a blank full name, or a short password.
   */
  @Transactional
  UUID create(String loginName, String fullName, String password) {
    String login = loginName.strip();
    String name = fullName.strip();
    List<Refusal> refusals = new ArrayList<>();
    if (login.isEmpty()) {
      refusals.add(
          new Refusal(LOGIN_NAME, FieldProblem.REQUIRED, "The login name must not be blank."));
    } else if (login.chars().anyMatch(Character::isWhitespace)) {
      refusals.add(new Refusal(LOGIN_NAME, "MUST_BE_ONE_WORD", "The login name must be one word."));
    } else if (repository.findByLoginName(login).isPresent()) {
      refusals.add(loginNameTaken(login));
    }
    if (name.isEmpty()) {
      refusals.add(
          new Refusal("fullName", FieldProblem.REQUIRED, "The full name must not be blank."));
    }
    if (password.length() < SHORTEST_PASSWORD) {
      refusals.add(
          new Refusal(
              "password",
              "TOO_SHORT",
              "The password must be at least " + SHORTEST_PASSWORD + " characters long."));
    }
    if (!refusals.isEmpty()) {
      throw new UserRefusedException(refusals);
    }
    User user =
        new User(UUID.randomUUID(), login, name, passwordEncoder.encode(password), clock.instant());
    try {
      return repository.saveAndFlush(user).id();
    } catch (DataIntegrityViolationException failed) {
      // Another user took the login name since the check above.
      if (failed.getCause() instanceof ConstraintViolationException violation
          && LOGIN_NAME_KEY.equals(violation.getConstraintName())) {
        throw new UserRefusedException(List.of(loginNameTaken(login)));
      }
      throw failed;
    }
  }

  private static Refusal loginNameTaken(String loginName) {
    return new Refusal(
        LOGIN_NAME, "LOGIN_NAME_TAKEN", "The login name " + loginName + " is taken.");
  }

  /** A rule a new user's field breaks: the field, its stable code (ENG-82) and why, in English. */
  record Refusal(String field, String code, String message) {}

  /**
   * A user that cannot be created, with why. A refusal about the new user's fields names each field
   * it breaks; any other, such as a first user when one exists, names none.
   */
  static final class UserRefusedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("serial") // An immutable list of records, never serialized in practice.
    private final List<Refusal> refusals;

    UserRefusedException(String message) {
      super(message);
      this.refusals = List.of();
    }

    UserRefusedException(List<Refusal> refusals) {
      super(refusals.stream().map(Refusal::message).collect(Collectors.joining(" ")));
      this.refusals = List.copyOf(refusals);
    }

    List<Refusal> refusals() {
      return refusals;
    }
  }
}
