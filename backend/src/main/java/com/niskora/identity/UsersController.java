package com.niskora.identity;

import com.niskora.common.ExpectedVersion;
import com.niskora.common.FieldProblem;
import com.niskora.common.PagedList;
import com.niskora.common.Problems;
import com.niskora.common.Sorting;
import com.niskora.common.Sorting.SortField;
import com.niskora.identity.Users.NoSuchUserException;
import com.niskora.identity.Users.SlowedDownException;
import com.niskora.identity.Users.UserRefusedException;
import com.niskora.identity.Users.VersionMismatchException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Users screen's accounts (PLT-21, FE-37): every account as a paged list, adding one, and
 * changing one with the verbs of ENG-70: resetting a password, disabling and enabling, and changing
 * your own password (PLT-22). Each change names the version it is based on (ENG-87). There is no
 * delete: an account is never deleted (PLT-45).
 */
@RestController
@RequestMapping(path = UsersController.PATH, produces = MediaType.APPLICATION_JSON_VALUE)
class UsersController {

  static final String PATH = "/api/identity/users";

  /** The fields the list sorts by, and the order of each: text whatever its case. */
  private static final Map<String, Function<Direction, Order>> SORT_FIELDS =
      Map.of(
          "loginName", direction -> new Order(direction, "loginName").ignoreCase(),
          "fullName", direction -> new Order(direction, "fullName").ignoreCase(),
          "createdAt", direction -> new Order(direction, "createdAt"));

  /** Last, so accounts that tie keep one order from page to page. */
  private static final Order TIE_BREAKER = Order.asc("id");

  private final UserRepository repository;
  private final Users users;

  UsersController(UserRepository repository, Users users) {
    this.repository = repository;
    this.users = users;
  }

  /**
   * Every account, a page at a time (ENG-76, ENG-77). {@code sort} takes {@code loginName}, {@code
   * fullName} and {@code createdAt}, each with a minus for descending (ENG-78); by full name when
   * left out. An unknown field is a 400 (ENG-79).
   */
  @GetMapping
  PagedList<UserResponse> listUsers(
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(required = false) @Nullable String sort) {
    List<Order> orders = new ArrayList<>();
    List<SortField> fields = Sorting.parse(sort, SORT_FIELDS.keySet());
    if (fields.isEmpty()) {
      fields = List.of(new SortField("fullName", false));
    }
    for (SortField field : fields) {
      // Sorting admits only the names it was given, so each has its order.
      Function<Direction, Order> order = Objects.requireNonNull(SORT_FIELDS.get(field.name()));
      orders.add(order.apply(field.descending() ? Direction.DESC : Direction.ASC));
    }
    orders.add(TIE_BREAKER);
    Pageable request = PageRequest.of(page - 1, PagedList.PAGE_SIZE, Sort.by(orders));
    return PagedList.of(repository.findAll(request), UserResponse::of);
  }

  /** One account, with its version as its {@code ETag} (ENG-87). */
  @GetMapping("/{id}")
  ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
    return repository
        .findById(id)
        .map(user -> withVersion(ResponseEntity.ok(), user))
        .orElseThrow(() -> new ErrorResponseException(HttpStatus.NOT_FOUND));
  }

  /**
   * Gives another user's account a new password (PLT-21), which niskora never mails (PLT-18). A
   * user changes their own with {@code change-password}, which asks for the current one.
   */
  @PostMapping(path = "/{id}/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<UserResponse> resetPassword(
      @PathVariable UUID id,
      ExpectedVersion version,
      @Valid @RequestBody PasswordResetRequest reset,
      @AuthenticationPrincipal LoggedInUser me) {
    return answerChange(() -> users.resetPassword(id, version, reset.password(), me.id()));
  }

  /**
   * Disables another user's account: it can no longer log in, and its open sessions end at once
   * (PLT-45). Nobody can disable their own, so at least one account can always log in.
   */
  @PostMapping("/{id}/disable")
  ResponseEntity<UserResponse> disableUser(
      @PathVariable UUID id, ExpectedVersion version, @AuthenticationPrincipal LoggedInUser me) {
    return answerChange(() -> users.disable(id, version, me.id()));
  }

  /** Enables a disabled account again, so it can log in (PLT-21). */
  @PostMapping("/{id}/enable")
  ResponseEntity<UserResponse> enableUser(
      @PathVariable UUID id, ExpectedVersion version, @AuthenticationPrincipal LoggedInUser me) {
    return answerChange(() -> users.enable(id, version, me.id()));
  }

  /**
   * Changes the logged-in user's own password (PLT-22), once they give their current one. A wrong
   * current password counts as a failed login (PLT-24).
   */
  @PostMapping(path = "/{id}/change-password", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<UserResponse> changePassword(
      @PathVariable UUID id,
      ExpectedVersion version,
      @Valid @RequestBody PasswordChangeRequest change,
      @AuthenticationPrincipal LoggedInUser me) {
    return answerChange(() ->
        users.changeOwnPassword(
            id, version, change.currentPassword(), change.newPassword(), me.id()));
  }

  /**
   * A change's answer: the account with its new version (ENG-81), or why it was refused: an unknown
   * account is a 404, an older version a 412 that says who changed it and when (ENG-87), and a
   * broken rule a 422 (ENG-82), and a password check while they are slowed down a 429 (PLT-24).
   */
  private static ResponseEntity<UserResponse> answerChange(Supplier<User> change) {
    User user;
    try {
      user = change.get();
    } catch (NoSuchUserException missing) {
      throw new ErrorResponseException(
          HttpStatus.NOT_FOUND, Problems.of(HttpStatus.NOT_FOUND), missing);
    } catch (VersionMismatchException mismatch) {
      throw new ErrorResponseException(
          HttpStatus.PRECONDITION_FAILED,
          Problems.versionMismatch(mismatch.changedAt(), mismatch.changedBy()),
          mismatch);
    } catch (UserRefusedException refused) {
      throw refusal(refused);
    } catch (SlowedDownException slowedDown) {
      throw Logins.tooManyAttempts(slowedDown.waitFor());
    }
    return withVersion(ResponseEntity.ok(), user);
  }

  /** A refused account as a 422: with every field error, or with a code of its own (ENG-82). */
  private static ErrorResponseException refusal(UserRefusedException refused) {
    HttpStatus status = HttpStatus.UNPROCESSABLE_CONTENT;
    ProblemDetail problem =
        refused.refusals().isEmpty()
            ? Problems.of(status, refused.code(), String.valueOf(refused.getMessage()))
            : Problems.ofFields(
                status,
                refused.refusals().stream()
                    .map(refusal ->
                        FieldProblem.at(refusal.field(), refusal.code(), refusal.message()))
                    .toList());
    return new ErrorResponseException(status, problem, refused);
  }

  /** An account's answer, with its version as the {@code ETag} (ENG-81, ENG-87). */
  private static ResponseEntity<UserResponse> withVersion(
      ResponseEntity.BodyBuilder answer, User user) {
    return answer.eTag(Long.toString(user.version())).body(UserResponse.of(user));
  }

  /**
   * Adds an account (PLT-21), answered with a 201 and its {@code Location}. A blank field is a 400,
   * and a rule the account breaks, such as a taken login name, a 422 (ENG-81), each with its field
   * errors (ENG-82).
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  ResponseEntity<UserResponse> createUser(@Valid @RequestBody NewUserRequest user) {
    UUID id;
    try {
      id = users.create(user.loginName(), user.fullName(), user.password());
    } catch (UserRefusedException refused) {
      throw refusal(refused);
    }
    User created = repository.findById(id).orElseThrow();
    return withVersion(ResponseEntity.created(URI.create(PATH + "/" + id)), created);
  }

  /** A new account: who the person is, the name they log in with and their first password. */
  record NewUserRequest(
      @NotBlank String loginName, @NotBlank String fullName, @NotBlank String password) {}

  /** Another user's new password. */
  record PasswordResetRequest(@NotBlank String password) {}

  /** The logged-in user's current password, which they must know, and their new one. */
  record PasswordChangeRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}

  /**
   * An account as the Users screen lists it, with its version, which a change sends back as {@code
   * If-Match} (ENG-87), and whether it is disabled (PLT-45).
   */
  record UserResponse(
      UUID id,
      String loginName,
      String fullName,
      Instant createdAt,
      long version,
      boolean isDisabled) {

    static UserResponse of(User user) {
      return new UserResponse(
          user.id(),
          user.loginName(),
          user.fullName(),
          user.createdAt(),
          user.version(),
          user.isDisabled());
    }
  }
}
