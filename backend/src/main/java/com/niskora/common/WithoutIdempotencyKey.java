package com.niskora.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the one kind of {@code POST} that takes no {@code Idempotency-Key}: one whose repeat is
 * harmless and whose stored answer must never be replayed, as logging in (ENG-85). Every other
 * {@code POST} under {@code /api} requires the key.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WithoutIdempotencyKey {

  /** Why a repeat is harmless, for the reader. */
  String value();
}
