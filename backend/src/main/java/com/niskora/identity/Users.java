package com.niskora.identity;

import com.niskora.common.ChangedBy;
import com.niskora.common.ExpectedVersion;
import com.niskora.common.FieldProblem;
import com.niskora.common.Problems;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and changes users, with their password hashed by Argon2 (PLT-17). A change names the
 * version it is based on, and is refused when the account has moved past it (ENG-87).
 */
@Service
class Users {

  /** The shortest password a user may have. */
  static final int SHORTEST_PASSWORD = 8;

  private static final String LOGIN_NAME = "loginName";

  /** The unique index that keeps two users from sharing a login name, whatever its case. */
  private static final String LOGIN_NAME_KEY = "users_login_name_key";

  private final UserRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final LoginThrottle throttle;
  private final Clock clock;

  Users(
      UserRepository repository,
      PasswordEncoder passwordEncoder,
      LoginThrottle throttle,
      Clock clock) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.throttle = throttle;
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
      throw new UserRefusedException(
          "FIRST_USER_EXISTS", "A user already exists, so this is not the first one.");
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
    checkLength("password", password, refusals);
    refuseIfAny(refusals);
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

  /**
   * Gives another user's account a new password (PLT-21). A user cannot reset their own: changing
   * it asks for the current one (PLT-22).
   */
  @Transactional
  User resetPassword(UUID id, ExpectedVersion version, String password, UUID by) {
    User user = forChange(id);
    if (id.equals(by)) {
      throw new UserRefusedException(
          "CANNOT_RESET_OWN_PASSWORD",
          "A user changes their own password, giving the current one.");
    }
    checkVersion(user, version);
    List<Refusal> refusals = new ArrayList<>();
    checkLength("password", password, refusals);
    refuseIfAny(refusals);
    user.setPasswordHash(passwordEncoder.encode(password), by, now());
    return repository.saveAndFlush(user);
  }

  /**
   * Changes the logged-in user's own password (PLT-22), once they give their current one. A wrong
   * current password counts as a failed login of their login name, so guessing it is slowed down as
   * guessing at Login is (PLT-24).
   */
  @Transactional
  User changeOwnPassword(
      UUID id, ExpectedVersion version, String currentPassword, String newPassword, UUID by) {
    User user = forChange(id);
    if (!id.equals(by)) {
      throw new UserRefusedException(
          "CAN_ONLY_CHANGE_OWN_PASSWORD",
          "A user changes only their own password; another user's is reset.");
    }
    checkVersion(user, version);
    Optional<Duration> wait = throttle.attempt(user.loginName());
    if (wait.isPresent()) {
      throw new SlowedDownException(wait.get());
    }
    List<Refusal> refusals = new ArrayList<>();
    if (passwordEncoder.matches(currentPassword, user.passwordHash())) {
      throttle.succeeded(user.loginName());
    } else {
      refusals.add(
          new Refusal("currentPassword", "WRONG_PASSWORD", "The current password is wrong."));
    }
    checkLength("newPassword", newPassword, refusals);
    refuseIfAny(refusals);
    user.setPasswordHash(passwordEncoder.encode(newPassword), by, now());
    return repository.saveAndFlush(user);
  }

  /**
   * Disables another user's account (PLT-45): it cannot log in, and its open sessions end at once,
   * on their next request. Nobody can disable their own, so at least one account can always log in.
   * An account already disabled is left as it is.
   */
  @Transactional
  User disable(UUID id, ExpectedVersion version, UUID by) {
    User user = forChange(id);
    if (id.equals(by)) {
      throw new UserRefusedException(
          "CANNOT_DISABLE_OWN_ACCOUNT", "Nobody can disable their own account.");
    }
    return setDisabled(user, version, true, by);
  }

  /** Enables a disabled account, so it can log in again (PLT-21). */
  @Transactional
  User enable(UUID id, ExpectedVersion version, UUID by) {
    return setDisabled(forChange(id), version, false, by);
  }

  private User setDisabled(User user, ExpectedVersion version, boolean disabled, UUID by) {
    checkVersion(user, version);
    if (user.isDisabled() == disabled) {
      return user;
    }
    user.setDisabled(disabled, by, now());
    return repository.saveAndFlush(user);
  }

  /** The user to change, with their row locked until the change is saved. */
  private User forChange(UUID id) {
    return repository.findForChange(id).orElseThrow(() -> new NoSuchUserException(id));
  }

  /** Refuses a change based on a version older than the account's, naming the latest change. */
  private void checkVersion(User user, ExpectedVersion version) {
    if (version.matches(user.version())) {
      return;
    }
    UUID changer = user.changedBy();
    ChangedBy changedBy =
        changer == null
            ? null
            : repository
                .findById(changer)
                .map(found -> new ChangedBy(found.id(), found.fullName()))
                .orElse(null);
    throw new VersionMismatchException(user.changedAt(), changedBy);
  }

  /** Refuses with every rule the request's fields break at once, when they break any (ENG-82). */
  private static void refuseIfAny(List<Refusal> refusals) {
    if (!refusals.isEmpty()) {
      throw new UserRefusedException(refusals);
    }
  }

  private static void checkLength(String field, String password, List<Refusal> refusals) {
    if (password.length() < SHORTEST_PASSWORD) {
      refusals.add(
          new Refusal(
              field,
              "TOO_SHORT",
              "The password must be at least " + SHORTEST_PASSWORD + " characters long."));
    }
  }

  /** Now, to the microsecond PostgreSQL keeps, so a change answers what a later read does. */
  private Instant now() {
    return clock.instant().truncatedTo(ChronoUnit.MICROS);
  }

  private static Refusal loginNameTaken(String loginName) {
    return new Refusal(
        LOGIN_NAME, "LOGIN_NAME_TAKEN", "The login name " + loginName + " is taken.");
  }

  /** A rule a new user's field breaks: the field, its stable code (ENG-82) and why, in English. */
  record Refusal(String field, String code, String message) {}

  /**
   * A user that cannot be created or changed, with why. A refusal about the request's fields names
   * each field it breaks, under the code of a validation error; any other, such as a first user
   * when one exists, has a code of its own (ENG-82).
   */
  static final class UserRefusedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    @SuppressWarnings("serial") // An immutable list of records, never serialized in practice.
    private final List<Refusal> refusals;

    UserRefusedException(String code, String message) {
      super(message);
      this.code = code;
      this.refusals = List.of();
    }

    UserRefusedException(List<Refusal> refusals) {
      super(refusals.stream().map(Refusal::message).collect(Collectors.joining(" ")));
      this.code = Problems.VALIDATION_FAILED;
      this.refusals = List.copyOf(refusals);
    }

    String code() {
      return code;
    }

    List<Refusal> refusals() {
      return refusals;
    }
  }

  /**
   * A password check refused for now, after too many wrong ones (PLT-24), with how long to wait.
   */
  static final class SlowedDownException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Duration wait;

    SlowedDownException(Duration wait) {
      super("Too many wrong passwords; wait " + wait + ".");
      this.wait = wait;
    }

    Duration waitFor() {
      return wait;
    }
  }

  /** A change to a user that does not exist. */
  static final class NoSuchUserException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    NoSuchUserException(UUID id) {
      super("No user has the id " + id + ".");
    }
  }

  /**
   * A change based on a version the account has moved past (ENG-87), with when the latest change
   * was made and by whom, null for one no user made.
   */
  static final class VersionMismatchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Instant changedAt;

    @SuppressWarnings("serial") // A record of an id and a name, never serialized in practice.
    private final @Nullable ChangedBy changedBy;

    VersionMismatchException(Instant changedAt, @Nullable ChangedBy changedBy) {
      super("The account was changed since the version this change is based on.");
      this.changedAt = changedAt;
      this.changedBy = changedBy;
    }

    Instant changedAt() {
      return changedAt;
    }

    @Nullable ChangedBy changedBy() {
      return changedBy;
    }
  }
}
