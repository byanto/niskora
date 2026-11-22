package com.niskora.common;

/**
 * The version of an editable resource that a change is based on (ENG-87): the one its caller read,
 * sent back in the {@code If-Match} header. A controller method takes it as a parameter, which
 * requires the header: a change without it is a 428, and one with anything but a version this
 * backend handed out as an {@code ETag}, such as {@code "3"}, a 400.
 *
 * <p>The module compares it with the resource's current version while it holds the resource's row,
 * and refuses a mismatch with {@link Problems#versionMismatch}.
 */
public record ExpectedVersion(long value) {

  /** Whether the resource is still at this version. */
  public boolean matches(long current) {
    return value == current;
  }
}
