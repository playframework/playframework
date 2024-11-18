/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static controllers.routes.Application;
import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.HttpVerbs.GET;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

import org.junit.Test;

public class IntegerParamTest extends AbstractRoutesTest {
  @Test
  public void checkBindFromQueryString() {
    testQueryParamBinding(
        "Integer",
        "/take-jint",
        "x=789",
        "789", // calls takeInteger(...)
        this::badRequestMissingParameter,
        this::badRequestMissingParameter);
    // Incorrect value
    var result = route(app, fakeRequest(GET, "/take-jint?x=a"));
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    // Reverse route
    assertThat(Application.takeInteger(789).url()).isEqualTo("/take-jint?x=789");
  }

  @Test
  public void checkBindOptionalFromQueryString() {
    testQueryParamBinding(
        "Optional[Integer]",
        "/take-jint-jopt",
        "x=789",
        "789", // calls takeIntegerOptional(...)
        this::okEmptyOptional,
        this::okEmptyOptional);
  }

  @Test
  public void checkBindFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "Integer",
        "/take-jint",
        "x=789",
        "789", // calls takeIntegerWithDefault(...)
        this.okContains("123"),
        this.okContains("123"));
  }

  @Test
  public void checkBindOptionalFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "Optional[Integer]",
        "/take-jint-jopt",
        "x=789",
        "789", // calls takeIntegerOptionalWithDefault(...)
        this.okContains("123"),
        this.okContains("123"));
  }

  @Test
  public void checkBindListFromQueryString() {
    testQueryParamBinding(
        "List[Integer]",
        "/take-jlist-jint",
        "x=7&x=8&x=9",
        "7,8,9", // calls takeJavaListInteger(...)
        this::okEmpty, // means empty list List() was passed to action
        this::okEmpty // means empty list List() was passed to action
        );
  }

  @Test
  public void checkBindOptionalListFromQueryString() {
    testQueryParamBinding(
        "Optional[List[Integer]]",
        "/take-jlist-jint-jopt",
        "x=7&x=8&x=9",
        "7,8,9", // calls takeJavaListIntegerOptional(...)
        this::okEmpty, // means empty list Optional.of(List()) was passed to action
        this::okEmpty // means empty list Optional.of(List()) was passed to action
        );
  }

  @Test
  public void checkBindListFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "List[Integer]",
        "/take-jlist-jint",
        "x=7&x=8&x=9",
        "7,8,9", // calls takeJavaListIntegerWithDefault(...)
        this.okContains("1,2,3"),
        this.okContains("1,2,3"));
  }

  @Test
  public void checkBindOptionalListFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "Optional[List[Integer]]",
        "/take-jlist-jint-jopt",
        "x=7&x=8&x=9",
        "7,8,9", // calls takeJavaListIntegerOptionalWithDefault(...)
        this.okContains("1,2,3"),
        this.okContains("1,2,3"));
  }
}
