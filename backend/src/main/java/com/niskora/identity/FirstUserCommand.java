package com.niskora.identity;

import com.niskora.identity.Users.UserRefusedException;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * Creates the first user from the command line on the server (PLT-23). Every later user is added on
 * the Users screen, so it refuses once any user exists. There is no self-signup (PLT-18).
 *
 * <p>{@code java -jar app.jar create-first-user --login-name=rina --full-name="Rina Wulandari"}
 * asks for the password twice on a terminal, or reads it from the first line of standard input when
 * piped, so it never shows in the shell's history or the process list. The backend then starts
 * without its web server and exits with {@link #getExitCode()}.
 */
@Component
public final class FirstUserCommand implements ApplicationRunner, ExitCodeGenerator {

  /**
   * The command's name, the first argument, which also tells the backend to start without serving.
   */
  public static final String NAME = "create-first-user";

  private static final String USAGE =
      "Usage: " + NAME + " --login-name=<login name> --full-name=<full name>";

  /** Reads the new user's password. */
  @FunctionalInterface
  interface PasswordReader {
    String read();
  }

  private final Users users;
  private final PasswordReader passwordReader;
  private int exitCode;

  @Autowired
  FirstUserCommand(Users users) {
    this(users, FirstUserCommand::readPassword);
  }

  FirstUserCommand(Users users, PasswordReader passwordReader) {
    this.users = users;
    this.passwordReader = passwordReader;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!args.getNonOptionArgs().equals(List.of(NAME))) {
      return;
    }
    String loginName = single(args, "login-name");
    String fullName = single(args, "full-name");
    if (loginName == null || fullName == null) {
      System.err.println(USAGE);
      exitCode = 2;
      return;
    }
    try {
      users.createFirstUser(loginName, fullName, passwordReader.read());
      System.out.println("Created the first user, " + loginName.strip() + ".");
      exitCode = 0;
    } catch (UserRefusedException refused) {
      System.err.println(refused.getMessage());
      exitCode = 1;
    }
  }

  @Override
  public int getExitCode() {
    return exitCode;
  }

  private static @Nullable String single(ApplicationArguments args, String option) {
    List<String> values = args.getOptionValues(option);
    return values == null || values.size() != 1 ? null : values.getFirst();
  }

  // Error Prone says System.console() is never null since JDK 22, but the JRE in the image does
  // return null when standard input is piped, as in `docker compose exec -T`.
  @SuppressWarnings("SystemConsoleNull")
  private static String readPassword() {
    Console console = System.console();
    if (console != null && console.isTerminal()) {
      char[] password = console.readPassword("Password for the new user: ");
      char[] again = console.readPassword("The same password again: ");
      if (password == null || again == null || !Arrays.equals(password, again)) {
        throw new UserRefusedException("PASSWORDS_DIFFER", "The two passwords differ.");
      }
      return new String(password);
    }
    try {
      // Not closed: it is the process's standard input.
      String line =
          new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
      if (line == null) {
        throw new UserRefusedException("NO_PASSWORD", "No password was given on standard input.");
      }
      return line;
    } catch (IOException failed) {
      throw new UncheckedIOException(failed);
    }
  }
}
