package com.niskora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

// common is technical and every module uses it (ENG-11), so a module started alone in a test also
// gets common's beans.
@SpringBootApplication
@Modulithic(sharedModules = "common")
public class NiskoraApplication {

  public static void main(String[] args) {
    SpringApplication.run(NiskoraApplication.class, args);
  }
}
