package com.niskora.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The one real clock, on UTC (ENG-64). A test replaces it with a fixed one. */
@Configuration(proxyBeanMethods = false)
class TimeConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  BusinessClock businessClock(Clock clock) {
    return new BusinessClock(clock);
  }
}
