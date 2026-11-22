package com.niskora.common.web;

import com.niskora.common.ExpectedVersion;
import com.niskora.common.Problems;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Reads the {@link ExpectedVersion} of a change from its {@code If-Match} header (ENG-87). A change
 * without the header is a 428. A header that holds anything but one version as this backend hands
 * it out, a strong entity tag of digits such as {@code "3"}, is a 400: {@code *} or a weak tag
 * names no version the change could be checked against.
 */
class ExpectedVersionResolver implements HandlerMethodArgumentResolver {

  static final String HEADER = HttpHeaders.IF_MATCH;

  private static final Pattern VERSION = Pattern.compile("\"(\\d{1,18})\"");

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType() == ExpectedVersion.class;
  }

  @Override
  public ExpectedVersion resolveArgument(
      MethodParameter parameter,
      @Nullable ModelAndViewContainer container,
      NativeWebRequest request,
      @Nullable WebDataBinderFactory binderFactory) {
    String ifMatch = request.getHeader(HEADER);
    if (ifMatch == null || ifMatch.isBlank()) {
      throw refused(
          HttpStatus.PRECONDITION_REQUIRED,
          "IF_MATCH_REQUIRED",
          "This change requires an If-Match header holding the ETag that was read.");
    }
    Matcher version = VERSION.matcher(ifMatch.strip());
    if (!version.matches()) {
      throw refused(
          HttpStatus.BAD_REQUEST,
          "IF_MATCH_INVALID",
          "If-Match must hold one ETag as it was read, such as \"3\".");
    }
    return new ExpectedVersion(Long.parseLong(version.group(1)));
  }

  private static ErrorResponseException refused(HttpStatus status, String code, String detail) {
    return new ErrorResponseException(status, Problems.of(status, code, detail), null);
  }
}
