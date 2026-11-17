package com.niskora.common.web;

import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
class BuildIdConfiguration {

  @Bean
  FilterRegistrationBean<BuildIdFilter> buildIdFilter(
      @Value("${niskora.build-id}") String buildId) {
    return registration(buildId);
  }

  /**
   * First in the chain and on every dispatch, so an error the servlet container forwards to the
   * error page also carries the build id. The image build sets it from the commit hash.
   */
  static FilterRegistrationBean<BuildIdFilter> registration(String buildId) {
    FilterRegistrationBean<BuildIdFilter> registration =
        new FilterRegistrationBean<>(new BuildIdFilter(buildId));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
    return registration;
  }
}
