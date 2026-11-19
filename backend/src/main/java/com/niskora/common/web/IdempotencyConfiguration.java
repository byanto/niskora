package com.niskora.common.web;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Repeated requests (ENG-85, ENG-86), only when the backend serves: a command such as
 * create-first-user runs without a server.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableScheduling
class IdempotencyConfiguration implements WebMvcConfigurer {

  private static final String API = "/api/";

  private final IdempotencyKeys keys;
  private final PlatformTransactionManager transactions;
  private final JsonMapper json;

  IdempotencyConfiguration(
      JdbcClient jdbc, JsonMapper json, Clock clock, PlatformTransactionManager transactions) {
    this.keys = new IdempotencyKeys(jdbc, json, clock);
    this.transactions = transactions;
    this.json = json;
  }

  /** The store, as a bean so its hourly clean-up is scheduled. */
  @Bean
  IdempotencyKeys idempotencyKeys() {
    return keys;
  }

  /** Last in the chain, after Spring Security, so it knows who sent the request. */
  @Bean
  FilterRegistrationBean<IdempotencyFilter> idempotencyFilter() {
    FilterRegistrationBean<IdempotencyFilter> registration =
        new FilterRegistrationBean<>(new IdempotencyFilter());
    registration.addUrlPatterns(API + "*");
    registration.setOrder(Ordered.LOWEST_PRECEDENCE);
    return registration;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(new IdempotencyInterceptor(keys, transactions, json))
        .addPathPatterns(API + "**");
  }
}
