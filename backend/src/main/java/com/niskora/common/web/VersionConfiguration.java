package com.niskora.common.web;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Lets a controller method take the {@code ExpectedVersion} of a change (ENG-87). */
@Configuration(proxyBeanMethods = false)
class VersionConfiguration implements WebMvcConfigurer {

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new ExpectedVersionResolver());
  }
}
