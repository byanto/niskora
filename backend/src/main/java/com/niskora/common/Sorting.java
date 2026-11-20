package com.niskora.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

/** The {@code sort} parameter of a paged list (ENG-78). */
public final class Sorting {

  private Sorting() {}

  /**
   * Reads a comma-separated list of fields, each with a minus for descending: {@code
   * -orderedAt,code}. Left out or empty, it is no sort, and the list keeps its own order. A field
   * the list cannot be sorted by, or one named twice, is a 400 and is never ignored (ENG-79).
   */
  public static List<SortField> parse(@Nullable String sort, Set<String> fields) {
    if (sort == null || sort.isEmpty()) {
      return List.of();
    }
    List<SortField> parsed = new ArrayList<>();
    Set<String> named = new HashSet<>();
    // A limit of -1 keeps a trailing empty entry, so "code," is refused rather than read as "code".
    for (String entry : sort.split(",", -1)) {
      boolean descending = entry.startsWith("-");
      String name = descending ? entry.substring(1) : entry;
      if (!fields.contains(name)) {
        throw badRequest("Unknown sort field: " + name + ".");
      }
      if (!named.add(name)) {
        throw badRequest("Sort field named twice: " + name + ".");
      }
      parsed.add(new SortField(name, descending));
    }
    return List.copyOf(parsed);
  }

  private static ErrorResponseException badRequest(String detail) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    return new ErrorResponseException(
        status, Problems.of(status, Problems.codeOf(status), detail), null);
  }

  /** One field to sort by, and whether descending. */
  public record SortField(String name, boolean descending) {}
}
