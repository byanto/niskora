package com.niskora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.modulith.Modulithic;

// common is technical and every module uses it (ENG-11), so a module started alone in a test also
// gets common's beans.
//
// This is @SpringBootApplication spelled out, so the scan can also leave out the configuration that
// belongs to Actuator's own context: Spring Boot loads that into the child context by itself.
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
    excludeFilters = {
      @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
      @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
      @Filter(ManagementContextConfiguration.class)
    })
@Modulithic(sharedModules = "common")
public class NiskoraApplication {

  public static void main(String[] args) {
    SpringApplication.run(NiskoraApplication.class, args);
  }
}
