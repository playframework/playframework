/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import org.junit.Test;

public class CharacterParamTest extends AbstractRoutesTest {

  @Test
  public void checkBindFromQueryString() {
    testQueryParamBinding(
        "Character",
        "/take-jchar",
        "x=z",
        "z", // calls takeCharacter(...)
        this::badRequestMissingParameter,
        this::badRequestMissingParameter);
  }

  @Test
  public void checkBindOptionalFromQueryString() {
    testQueryParamBinding(
        "Optional[Character]",
        "/take-jchar-jopt",
        "x=z",
        "z", // calls takeCharacterOptional(...)
        this::okEmptyOptional,
        this::okEmptyOptional);
  }

  @Test
  public void checkBindFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "Character",
        "/take-jchar",
        "x=z",
        "z", // calls takeCharacterWithDefault(...)
        this.okContains("a"),
        this.okContains("a"));
  }

  @Test
  public void checkBindOptionalFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "Optional[Character]",
        "/take-jchar-jopt",
        "x=z",
        "z", // calls takeCharacterOptionalWithDefault(...)
        this.okContains("a"),
        this.okContains("a"));
  }

  @Test
  public void checkBindListFromQueryString() {
    testQueryParamBinding(
        "List[Character]",
        "/take-jlist-jchar",
        "x=z",
        "z", // calls takeJavaListCharacter(...)
        this::okEmpty, // means empty List() was passed to action
        this::okEmpty); // means empty List() was passed to action
  }

  @Test
  public void checkBindOptionalListFromQueryString() {
    testQueryParamBinding(
        "Optional[List[Character]]",
        "/take-jlist-jchar-jopt",
        "x=z",
        "z", // calls takeJavaListCharacterOptional(...)
        this::okEmpty, // means empty Optional.of(List()) was passed to action
        this::okEmpty); // means empty Optional.of(List()) was passed to action
  }

  @Test
  public void checkBindListFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "List[Character]",
        "/take-jlist-jchar",
        "x=z",
        "z", // calls takeJavaListCharacterWithDefault(...)
        this.okContains("a,b,c"),
        this.okContains("a,b,c"));
  }

  @Test
  public void checkBindOptionalListFromQueryStringWithDefault() {
    testQueryParamBindingWithDefault(
        "Optional[List[Character]]",
        "/take-jlist-jchar-jopt",
        "x=z",
        "z", // calls takeJavaListCharacterOptionalWithDefault(...)
        this.okContains("a,b,c"),
        this.okContains("a,b,c"));
  }
}
