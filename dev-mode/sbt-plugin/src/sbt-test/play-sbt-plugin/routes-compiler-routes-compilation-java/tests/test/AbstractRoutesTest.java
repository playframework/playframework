/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

import java.util.function.BiConsumer;
import play.mvc.Result;
import play.test.WithApplication;

public abstract class AbstractRoutesTest extends WithApplication {

  protected void okEmpty(String url, Result result) {
    assertThat(result.status()).as("status " + url).isEqualTo(OK);
    assertThat(contentAsString(result)).as(url).isEmpty();
  }

  protected BiConsumer<String, Result> okContains(String content) {
    return (url, result) -> {
      assertThat(result.status()).as("status " + url).isEqualTo(OK);
      assertThat(contentAsString(result)).as(url).isEqualTo(content);
    };
  }

  protected void okEmptyOptional(String url, Result result) {
    okContains("emptyOptional").accept(url, result);
  }

  protected void badRequest(String url, Result result) {
    assertThat(result.status()).as("status " + url).isEqualTo(BAD_REQUEST);
  }

  protected void notFound(String url, Result result) {
    assertThat(result.status()).as("status " + url).isEqualTo(NOT_FOUND);
  }

  protected void badRequestMissingParameter(String url, Result result) {
    assertThat(result.status()).as("status " + url).isEqualTo(BAD_REQUEST);
    assertThat(contentAsString(result)).as(url).contains("Missing parameter: x");
  }

  protected void badRequestCannotParseParameter(String url, Result result) {
    assertThat(result.status()).as("status " + url).isEqualTo(BAD_REQUEST);
    assertThat(contentAsString(result)).as(url).contains("Cannot parse parameter");
  }

  protected void checkResult(String path, String params, BiConsumer<String, Result> assertion) {
    checkResult(path + "?" + params, assertion);
  }

  protected void checkResult(String url, BiConsumer<String, Result> assertion) {
    var result = route(app, fakeRequest(GET, url));
    assertion.accept(url, result);
  }

  protected void testQueryParamBinding(
      String path,
      String params,
      String expectationSuccess,
      BiConsumer<String, Result> whenNoValue,
      BiConsumer<String, Result> whenNoParam) {
    testQueryParamBinding(path, params, okContains(expectationSuccess), whenNoValue, whenNoParam);
  }

  protected void testQueryParamBinding(
      String path,
      String params,
      BiConsumer<String, Result> expectation,
      BiConsumer<String, Result> whenNoValue,
      BiConsumer<String, Result> whenNoParam) {
    var url = String.format("%s?%s", path, params);
    var result = route(app, fakeRequest(GET, url));
    expectation.accept(url, result);
    // when there is a parameter but without value (=empty string)
    url = path + "?x=";
    result = route(app, fakeRequest(GET, url));
    whenNoValue.accept(url, result);
    // when there is a parameter but without value (=empty string) and without equals sign
    url = path + "?x";
    result = route(app, fakeRequest(GET, url));
    whenNoValue.accept(url, result);
    // when there is no parameter at all
    result = route(app, fakeRequest(GET, path));
    whenNoParam.accept(url, result);
  }
}
