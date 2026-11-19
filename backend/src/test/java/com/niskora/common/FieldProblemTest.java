package com.niskora.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** A field error points at its field with a JSON Pointer (ENG-82). */
class FieldProblemTest {

  @Test
  void pointsAtAFieldByItsPath() {
    assertThat(FieldProblem.at("loginName", "REQUIRED", "").pointer()).isEqualTo("/loginName");
    assertThat(FieldProblem.at("lines[2].quantity", "REQUIRED", "").pointer())
        .isEqualTo("/lines/2/quantity");
  }

  @Test
  void escapesTheCharactersAPointerReserves() {
    assertThat(FieldProblem.at("a/b.c~d", "REQUIRED", "").pointer()).isEqualTo("/a~1b/c~0d");
  }

  @Test
  void pointsAtTheWholeBodyForNoPath() {
    assertThat(FieldProblem.at("", "REQUIRED", "").pointer()).isEmpty();
  }
}
