package com.niskora.common.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * The build id on Actuator's own port as well, so the public health check that Caddy forwards there
 * carries it too (ENG-92). Actuator runs in a child context with its own server, which does not
 * pick up the application's filters. Spring Boot loads this class into that context from {@code
 * META-INF/spring}; the application's component scan leaves it out.
 */
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
class BuildIdManagementConfiguration {

  @Bean
  FilterRegistrationBean<BuildIdFilter> managementBuildIdFilter(
      @Value("${niskora.build-id}") String buildId) {
    return BuildIdConfiguration.registration(buildId);
  }
}
