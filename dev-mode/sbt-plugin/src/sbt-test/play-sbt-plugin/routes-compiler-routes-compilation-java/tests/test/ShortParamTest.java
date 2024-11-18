/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import org.junit.Test;

public class ShortParamTest extends AbstractRoutesTest {

  @Test
  public void checkBindFromQueryString() {
    testQueryParamBinding(
        "Short",
        "/take-jshort",
        "x=789",
        "789", // calls takeJavaShort(...)
        this::badRequestMissingParameter,
        this::badRequestMissingParameter);
  }

  @Test
  public void checkBindOptionalFromQueryString() {
    testQueryParamBinding(
        "Optional[Short]",
        "/take-jshort-jopt",
        "x=789",
        "789", // calls takeJavaShortOptional(...)
        this::okEmptyOptional,
        this::okEmptyOptional);
  }

  @Test
  public void checkBindFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "Short",
        "/take-jshort",
        "x=789",
        "789", // calls takeJavaShortWithDefault(...)
        this.okContains("123"),
        this.okContains("123"));
  }

  @Test
  public void checkBindOptionalFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "Optional[Short]",
        "/take-jshort-jopt",
        "x=789",
        "789", // calls takeJavaShortOptionalWithDefault(...)
        this.okContains("123"),
        this.okContains("123"));
  }

  @Test
  public void checkBindListFromQueryString() {
    testQueryParamBinding(
        "List[Short]",
        "/take-jlist-jshort",
        "x=7&x=8&x=9",
        "7,8,9", // calls takeJavaListShort(...)
        this::okEmpty, // means empty list List() was passed to action
        this::okEmpty); // means empty list List() was passed to action
  }

  @Test
  public void checkBindOptionalListFromQueryString() {
    testQueryParamBinding(
        "Optional[List[Short]]",
        "/take-jlist-jshort-jopt",
        "x=7&x=8&x=9",
        "7,8,9", // calls takeJavaListShortOptional(...)
        this::okEmpty, // means empty list Optional.of(List()) was passed to action
        this::okEmpty); // means empty list Optional.of(List()) was passed to action
  }

  @Test
  public void checkBindListFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "List[Short]",
        "/take-jlist-jshort",
        "x=7&x=8&x=9",
        "7,8,9", // calls takeJavaListShortWithDefault(...)
        this.okContains("1,2,3"),
        this.okContains("1,2,3"));
  }

  @Test
  public void checkBindOptionalListFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "Optional[List[Short]]",
        "/take-jlist-jshort-jopt",
        "x=7&x=8&x=9",
        "7,8,9", // calls takeJavaListShortOptionalWithDefault(...)
        this.okContains("1,2,3"),
        this.okContains("1,2,3"));
  }
}
