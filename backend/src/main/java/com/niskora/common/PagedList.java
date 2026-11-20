package com.niskora.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * One page of a paged list, the one shape every paged list answers with (ENG-76). Pages start at 1
 * and hold {@value #PAGE_SIZE} items, a size the server fixes (ENG-77).
 */
public record PagedList<T>(List<T> data, Pagination pagination) {

  /** How many items a page holds. It is not a parameter (ENG-77). */
  public static final int PAGE_SIZE = 50;

  /** A page that Spring Data read, with each item turned into what the API answers with. */
  public static <S, T> PagedList<T> of(Page<S> page, Function<? super S, T> item) {
    return new PagedList<>(
        page.getContent().stream().<T>map(item).toList(),
        new Pagination(
            page.getNumber() + 1, page.getSize(), page.getTotalElements(), page.getTotalPages()));
  }

  /** Where a page sits in its list: its number from 1, its size and the list's totals. */
  public record Pagination(int page, int pageSize, long totalItems, int totalPages) {}
}
