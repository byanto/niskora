package com.niskora.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.niskora.common.Sorting.SortField;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

/** The {@code sort} parameter of a paged list (ENG-78, ENG-79). */
class SortingTest {

  private static final Set<String> FIELDS = Set.of("orderedAt", "code");

  @Test
  void takesACommaSeparatedListWithAMinusForDescending() {
    assertThat(Sorting.parse("-orderedAt,code", FIELDS))
        .containsExactly(new SortField("orderedAt", true), new SortField("code", false));
  }

  @Test
  void leftOutOrEmptyIsNoSort() {
    assertThat(Sorting.parse(null, FIELDS)).isEmpty();
    assertThat(Sorting.parse("", FIELDS)).isEmpty();
  }

  @Test
  void anUnknownFieldIsABadRequest() {
    assertBadRequest("-placedAt", "Unknown sort field: placedAt.");
  }

  @Test
  void anEmptyEntryIsABadRequest() {
    assertBadRequest("code,", "Unknown sort field: .");
    assertBadRequest("-", "Unknown sort field: .");
  }

  @Test
  void aFieldNamedTwiceIsABadRequest() {
    assertBadRequest("code,-code", "Sort field named twice: code.");
  }

  private static void assertBadRequest(String sort, String detail) {
    assertThatThrownBy(() -> Sorting.parse(sort, FIELDS))
        .isInstanceOfSatisfying(
            ErrorResponseException.class,
            refused -> {
              assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(refused.getBody().getProperties()).containsEntry("code", "BAD_REQUEST");
              assertThat(refused.getBody().getDetail()).isEqualTo(detail);
            });
  }
}
