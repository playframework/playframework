/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import org.junit.Test;

public class StringParamTest extends AbstractRoutesTest {

  @Test
  public void checkBindFromQueryString() {
    testQueryParamBinding(
        "String",
        "/take-str",
        "x=xyz",
        "xyz", // calls takeString(...)
        this::okEmpty,
        this::badRequestMissingParameter);
  }

  @Test
  public void checkBindOptionalFromQueryString() {
    testQueryParamBinding(
        "Optional[String]",
        "/take-str-jopt",
        "x=xyz",
        "xyz", // calls takeStringOptional(...)
        this::okEmpty,
        this::okEmptyOptional);
  }

  @Test
  public void checkBindFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "String",
        "/take-str",
        "x=xyz",
        "xyz", // calls takeStringWithDefault(...)
        this::okEmpty,
        okContains("abc"));
  }

  @Test
  public void checkBindOptionalFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "Optional[String]",
        "/take-str-jopt",
        "x=xyz",
        "xyz", // calls takeStringOptionalWithDefault(...)
        this::okEmpty,
        okContains("abc"));
  }

  @Test
  public void checkBindListFromQueryString() {
    testQueryParamBinding(
        "List[String]",
        "/take-jlist-str",
        "x=x&x=y&x=z",
        "x,y,z", // calls takeJavaListString(...)
        this.okContains("emptyStringElement"), // means non-empty List("") was passed to action
        this::okEmpty); // means empty List() was passed to action
  }

  @Test
  public void checkBindOptionalListFromQueryString() {
    testQueryParamBinding(
        "Optional[List[String]]",
        "/take-jlist-str-jopt",
        "x=x&x=y&x=z",
        "x,y,z", // calls takeJavaListStringOptional(...)
        this.okContains(
            "emptyStringElement"), // means non-empty list Optional.of(List("")) was passed to
                                   // action
        this::okEmpty); // means empty list Optinal.of(List()) was passed to action
  }

  @Test
  public void checkBindListFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "List[String]",
        "/take-jlist-str",
        "x=x&x=y&x=z",
        "x,y,z", // calls takeJavaListStringWithDefault(...)
        this.okContains("emptyStringElement"), // means non-empty List("") was passed to action
        this.okContains("abc,def,ghi"));
  }

  @Test
  public void checkBindOptionalListFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "Optional[List[String]]",
        "/take-jlist-str-jopt",
        "x=x&x=y&x=z",
        "x,y,z", // calls takeJavaListStringOptionalWithDefault(...)
        this.okContains(
            "emptyStringElement"), // means non-empty list Optinal.of(List("")) was passed to action
        this.okContains("abc,def,ghi"));
  }
}
