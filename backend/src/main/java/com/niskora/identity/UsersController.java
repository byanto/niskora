package com.niskora.identity;

import com.niskora.common.FieldProblem;
import com.niskora.common.PagedList;
import com.niskora.common.Problems;
import com.niskora.common.Sorting;
import com.niskora.common.Sorting.SortField;
import com.niskora.identity.Users.UserRefusedException;
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
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The Users screen's accounts (PLT-21, FE-37): every account as a paged list, and adding one. */
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

  /** One account. */
  @GetMapping("/{id}")
  UserResponse getUser(@PathVariable UUID id) {
    return repository
        .findById(id)
        .map(UserResponse::of)
        .orElseThrow(() -> new ErrorResponseException(HttpStatus.NOT_FOUND));
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
      throw new ErrorResponseException(
          HttpStatus.UNPROCESSABLE_CONTENT,
          Problems.ofFields(
              HttpStatus.UNPROCESSABLE_CONTENT,
              refused.refusals().stream()
                  .map(refusal ->
                      new FieldProblem(
                          "/" + Objects.requireNonNull(refusal.field()),
                          refusal.code(),
                          refusal.message()))
                  .toList()),
          refused);
    }
    UserResponse created = getUser(id);
    return ResponseEntity.created(URI.create(PATH + "/" + id)).body(created);
  }

  /** A new account: who the person is, the name they log in with and their first password. */
  record NewUserRequest(
      @NotBlank String loginName, @NotBlank String fullName, @NotBlank String password) {}

  /** An account as the Users screen lists it. */
  record UserResponse(UUID id, String loginName, String fullName, Instant createdAt) {

    static UserResponse of(User user) {
      return new UserResponse(user.id(), user.loginName(), user.fullName(), user.createdAt());
    }
  }
}
