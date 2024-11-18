/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import org.junit.Test;

public class OptionalParamTest extends AbstractRoutesTest {

  @Test
  public void checkBindOptionalIntFromQueryString() {
    testQueryParamBinding(
        "OptionalInt",
        "/take-joptint",
        "x=789",
        "789", // calls takeOptionalInt(...)
        this.okContains("emptyOptionalInt"),
        this.okContains("emptyOptionalInt"));
  }

  @Test
  public void checkBindOptionalLongFromQueryString() {
    testQueryParamBinding(
        "OptionalLong",
        "/take-joptlong",
        "x=789",
        "789", // calls takeOptionalLong(...)
        this.okContains("emptyOptionalLong"),
        this.okContains("emptyOptionalLong"));
  }

  @Test
  public void checkBindOptionalDoubleFromQueryString() {
    testQueryParamBinding(
        "OptionalDouble",
        "/take-joptdouble",
        "x=7.89",
        "7.89", // calls takeOptionalDouble(...)
        this.okContains("emptyOptionalDouble"),
        this.okContains("emptyOptionalDouble"));
  }

  @Test
  public void checkBindOptionalIntFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "OptionalInt",
        "/take-joptint",
        "x=789",
        "789", // calls takeOptionalIntWithDefault(...)
        this.okContains("123"),
        this.okContains("123"));
  }

  @Test
  public void checkBindOptionalLongFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "OptionalLong",
        "/take-joptlong",
        "x=789",
        "789", // calls takeOptionalLongWithDefault(...)
        this.okContains("123"),
        this.okContains("123"));
  }

  @Test
  public void checkBindOptionalDoubleFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "OptionalDouble",
        "/take-joptdouble",
        "x=7.89",
        "7.89", // calls takeOptionalDoubleWithDefault(...)
        this.okContains("1.23"),
        this.okContains("1.23"));
  }
}
