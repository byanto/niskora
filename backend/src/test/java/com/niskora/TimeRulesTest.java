package com.niskora;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.niskora.common.BusinessClock;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

/** Code never reads the system time directly; it asks the injected clock (ENG-64). */
class TimeRulesTest {

  private static final JavaClasses PRODUCTION =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.niskora");

  @Test
  void noCodeReadsTheSystemTime() {
    noClasses()
        .should()
        .callMethod(System.class, "currentTimeMillis")
        .orShould()
        .callMethod(Instant.class, "now")
        .orShould()
        .callMethod(LocalDate.class, "now")
        .orShould()
        .callMethod(LocalDateTime.class, "now")
        .orShould()
        .callMethod(LocalTime.class, "now")
        .orShould()
        .callMethod(OffsetDateTime.class, "now")
        .orShould()
        .callMethod(ZonedDateTime.class, "now")
        .because("the time comes from the injected Clock or " + BusinessClock.class.getSimpleName())
        .check(PRODUCTION);
  }

  @Test
  void onlyTheTimeConfigurationMakesTheSystemClock() {
    noClasses()
        .that()
        .doNotHaveFullyQualifiedName("com.niskora.common.TimeConfiguration")
        .should()
        .callMethod(Clock.class, "systemUTC")
        .orShould()
        .callMethod(Clock.class, "systemDefaultZone")
        .because("every other class is given the one Clock bean")
        .check(PRODUCTION);
  }
}
