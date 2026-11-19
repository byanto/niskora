package com.niskora.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.niskora.DatabaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The first account is created with a command on the server (PLT-23), with its password hashed by
 * Argon2 (PLT-17). The annotations match {@code BackendStartsTest}, so both share one context.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DatabaseTest
@ExtendWith(OutputCaptureExtension.class)
class FirstUserCommandTest {

  private static final String PASSWORD = "correct horse battery";

  @Autowired Users users;

  @Autowired UserDetailsService userDetailsService;

  @Autowired PasswordEncoder passwordEncoder;

  @Test
  void createsTheFirstUserWithAnArgon2HashedPassword(CapturedOutput output) throws Exception {
    FirstUserCommand command = command();

    command.run(args("create-first-user", "--login-name=rina", "--full-name=Rina Wulandari"));

    assertThat(command.getExitCode()).isZero();
    assertThat(output.getOut()).contains("Created the first user, rina.");
    UserDetails user = userDetailsService.loadUserByUsername("rina");
    assertThat(user.getPassword()).startsWith("{argon2}$argon2id$");
    assertThat(passwordEncoder.matches(PASSWORD, user.getPassword())).isTrue();
    assertThat(((LoggedInUser) user).fullName()).isEqualTo("Rina Wulandari");
  }

  @Test
  void refusesOnceAUserExists(CapturedOutput output) throws Exception {
    users.create("sari", "Sari Dewi", PASSWORD);
    FirstUserCommand command = command();

    command.run(args("create-first-user", "--login-name=rina", "--full-name=Rina Wulandari"));

    assertThat(command.getExitCode()).isEqualTo(1);
    assertThat(output.getErr()).contains("A user already exists");
    assertThatThrownBy(() -> userDetailsService.loadUserByUsername("rina"))
        .isInstanceOf(UsernameNotFoundException.class);
  }

  @Test
  void refusesAPasswordThatIsTooShort(CapturedOutput output) throws Exception {
    FirstUserCommand command = new FirstUserCommand(users, () -> "short");

    command.run(args("create-first-user", "--login-name=rina", "--full-name=Rina Wulandari"));

    assertThat(command.getExitCode()).isEqualTo(1);
    assertThat(output.getErr()).contains("at least 8 characters");
  }

  @Test
  void explainsItselfWhenTheLoginNameOrFullNameIsMissing(CapturedOutput output) throws Exception {
    FirstUserCommand command = command();

    command.run(args("create-first-user", "--login-name=rina"));

    assertThat(command.getExitCode()).isEqualTo(2);
    assertThat(output.getErr()).contains("Usage: create-first-user --login-name=");
  }

  @Test
  void doesNothingWhenTheBackendStartsAsAServer() throws Exception {
    FirstUserCommand command = command();

    command.run(args());

    assertThat(command.getExitCode()).isZero();
    assertThatThrownBy(() -> userDetailsService.loadUserByUsername("rina"))
        .isInstanceOf(UsernameNotFoundException.class);
  }

  private FirstUserCommand command() {
    return new FirstUserCommand(users, () -> PASSWORD);
  }

  private static DefaultApplicationArguments args(String... args) {
    return new DefaultApplicationArguments(args);
  }
}
