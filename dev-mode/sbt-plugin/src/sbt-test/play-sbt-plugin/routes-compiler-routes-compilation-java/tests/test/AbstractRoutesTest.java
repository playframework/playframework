/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.BAD_REQUEST;
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
      assertThat(contentAsString(result)).as(url).contains(content);
    };
  }

  protected void okEmptyOptional(String url, Result result) {
    okContains("emptyOptional").accept(url, result);
  }

  protected void badRequestMissingParameter(String url, Result result) {
    assertThat(result.status()).as("status " + url).isEqualTo(BAD_REQUEST);
    assertThat(contentAsString(result)).as(url).contains("Missing parameter: x");
  }

  protected void testQueryParamBindingWithDefault(
      String paramType,
      String path,
      String successParams,
      String expectationSuccess,
      BiConsumer<String, Result> whenNoValue,
      BiConsumer<String, Result> whenNoParam) {
    testQueryParamBinding(
        paramType, path, successParams, expectationSuccess, whenNoValue, whenNoParam, true);
  }

  protected void testQueryParamBinding(
      String paramType,
      String path,
      String successParams,
      String successExpectation,
      BiConsumer<String, Result> whenNoValue,
      BiConsumer<String, Result> whenNoParam) {
    testQueryParamBinding(
        paramType, path, successParams, successExpectation, whenNoValue, whenNoParam, false);
  }

  protected void testQueryParamBinding(
      String paramType,
      String path,
      String successParams,
      String successExpectation,
      BiConsumer<String, Result> whenNoValue,
      BiConsumer<String, Result> whenNoParam,
      boolean withDefault) {
    var resolvedPath = path + (withDefault ? "-d" : "");
    var url = String.format("%s?%s", resolvedPath, successParams);
    var result = route(app, fakeRequest(GET, url));
    assertThat(result.status()).as("status " + url).isEqualTo(OK);
    assertThat(contentAsString(result)).as(url).isEqualTo(successExpectation);
    // when there is a parameter but without value (=empty string)
    url = resolvedPath + "?x=";
    result = route(app, fakeRequest(GET, url));
    whenNoValue.accept(url, result);
    // when there is a parameter but without value (=empty string) and without equals sign
    url = resolvedPath + "?x";
    result = route(app, fakeRequest(GET, url));
    whenNoValue.accept(url, result);
    // when there is no parameter at all
    result = route(app, fakeRequest(GET, resolvedPath));
    whenNoParam.accept(url, result);
  }
}
