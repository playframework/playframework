/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static controllers.routes.StringController;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import controllers.StringController;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class StringParamTest extends AbstractRoutesTest {

  private static final List<String> defaultList = List.of("abc", "def", "ghi");
  private static final List<String> testList = List.of("ab", "cd", "ef");
  private static final List<String> unicodeList = List.of("πε", "επ");

  /** Tests route {@code /str-p} {@link StringController#path}. */
  @Test
  public void checkPath() {
    var path = "/str-p";
    // Correct value
    checkResult(path + "/xyz", okContains("xyz"));
    checkResult(path + "/πε", okContains("πε"));
    checkResult(path + "/%CF%80%CE%B5", okContains("πε"));
    // Without param
    checkResult(path, this::notFound);
    // Reverse route
    assertThatThrownBy(() -> StringController.path(null).url())
        .isInstanceOf(NullPointerException.class);
    assertThat(StringController.path("xyz").url()).isEqualTo(path + "/xyz");
    assertThat(StringController.path("πε").url()).isEqualTo(path + "/%CF%80%CE%B5");
  }

  /** Tests route {@code /str} {@link StringController#query}. */
  @Test
  public void checkQuery() {
    var path = "/str";
    // Correct value
    checkResult(path, "x=xyz", okContains("xyz"));
    checkResult(path, "x=πε", okContains("πε"));
    checkResult(path, "x=%CF%80%CE%B5", okContains("πε"));
    // Without/Empty/NoValue param
    checkResult(path, this::badRequestMissingParameter);
    checkResult(path, "x", this::okEmpty);
    checkResult(path, "x=", this::okEmpty);
    // Reverse route
    assertThat(StringController.query(null).url()).isEqualTo(path + "?x=");
    assertThat(StringController.query("xyz").url()).isEqualTo(path + "?x=xyz");
    assertThat(StringController.query("πε").url()).isEqualTo(path + "?x=%CF%80%CE%B5");
  }

  /** Tests route {@code /str-d} {@link StringController#queryDefault}. */
  @Test
  public void checkQueryDefault() {
    var path = "/str-d";
    // Correct value
    checkResult(path, "x?%3D=xyz", okContains("xyz"));
    checkResult(path, "x?%3D=πε", okContains("πε"));
    checkResult(path, "x?%3D=%CF%80%CE%B5", okContains("πε"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("abc"));
    checkResult(path, "x?%3D", this::okEmpty);
    checkResult(path, "x?%3D=", this::okEmpty);
    // Reverse route
    assertThat(StringController.queryDefault(null).url()).isEqualTo(path + "?x%3F%3D=");
    assertThat(StringController.queryDefault("abc").url()).isEqualTo(path);
    assertThat(StringController.queryDefault("xyz").url()).isEqualTo(path + "?x%3F%3D=xyz");
    assertThat(StringController.queryDefault("πε").url()).isEqualTo(path + "?x%3F%3D=%CF%80%CE%B5");
  }

  /** Tests route {@code /str-f} {@link StringController#queryFixed}. */
  @Test
  public void checkQueryFixed() {
    var path = "/str-f";
    // Correct value
    checkResult(path, "x=xyz", okContains("abc"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("abc"));
    checkResult(path, "x", okContains("abc"));
    checkResult(path, "x=", okContains("abc"));
    // Reverse route
    assertThat(StringController.queryFixed().url()).isEqualTo(path);
  }

  /** Tests route {@code /str-null} {@link StringController#queryNullable}. */
  @Test
  public void checkQueryNullable() {
    var path = "/str-null";
    // Correct value
    checkResult(path, "x?=xyz", okContains("xyz"));
    checkResult(path, "x?=πε", okContains("πε"));
    checkResult(path, "x?=%CF%80%CE%B5", okContains("πε"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x?", this::okEmpty);
    checkResult(path, "x?=", this::okEmpty);
    // Reverse route
    assertThat(StringController.queryNullable(null).url()).isEqualTo(path);
    assertThat(StringController.queryNullable("xyz").url()).isEqualTo(path + "?x%3F=xyz");
    assertThat(StringController.queryNullable("πε").url()).isEqualTo(path + "?x%3F=%CF%80%CE%B5");
  }

  /** Tests route {@code /str-opt} {@link StringController#queryOptional}. */
  @Test
  public void checkQueryOptional() {
    var path = "/str-opt";
    // Correct value
    checkResult(path, "x?=xyz", okContains("xyz"));
    checkResult(path, "x?=πε", okContains("πε"));
    checkResult(path, "x?=%CF%80%CE%B5", okContains("πε"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmptyOptional);
    checkResult(path, "x?", this::okEmpty);
    checkResult(path, "x?=", this::okEmpty);
    // Reverse route
    assertThat(StringController.queryOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(StringController.queryOptional(Optional.of("xyz")).url())
        .isEqualTo(path + "?x%3F=xyz");
    assertThat(StringController.queryOptional(Optional.of("πε")).url())
        .isEqualTo(path + "?x%3F=%CF%80%CE%B5");
  }

  /** Tests route {@code /str-opt-d} {@link StringController#queryOptionalDefault}. */
  @Test
  public void checkQueryOptionalDefault() {
    var path = "/str-opt-d";
    // Correct value
    checkResult(path, "x?%3D=xyz", okContains("xyz"));
    checkResult(path, "x?%3D=πε", okContains("πε"));
    checkResult(path, "x?%3D=%CF%80%CE%B5", okContains("πε"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("abc"));
    checkResult(path, "x?%3D", this::okEmpty);
    checkResult(path, "x?%3D=", this::okEmpty);
    // Reverse route
    assertThat(StringController.queryOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(StringController.queryOptionalDefault(Optional.of("abc")).url()).isEqualTo(path);
    assertThat(StringController.queryOptionalDefault(Optional.of("xyz")).url())
        .isEqualTo(path + "?x%3F%3D=xyz");
    assertThat(StringController.queryOptionalDefault(Optional.of("πε")).url())
        .isEqualTo(path + "?x%3F%3D=%CF%80%CE%B5");
  }

  /** Tests route {@code /str-list} {@link StringController#queryList}. */
  @Test
  public void checkQueryList() {
    var path = "/str-list";
    // Correct value
    checkResult(path, "x[]=ab&x[]=cd&x[]=ef", okContains("ab,cd,ef"));
    checkResult(path, "x[]=πε&x[]=%CE%B5%CF%80&x[]=", okContains("πε,επ,empty"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]", okContains("empty"));
    checkResult(path, "x[]=", okContains("empty"));
    // Reverse route
    assertThat(StringController.queryList(List.of()).url()).isEqualTo(path);
    assertThat(StringController.queryList(testList).url())
        .isEqualTo(path + "?x%5B%5D=ab&x%5B%5D=cd&x%5B%5D=ef");
    assertThat(StringController.queryList(unicodeList).url())
        .isEqualTo(path + "?x%5B%5D=%CF%80%CE%B5&x%5B%5D=%CE%B5%CF%80");
  }

  /** Tests route {@code /str-list-d} {@link StringController#queryListDefault}. */
  @Test
  public void checkQueryListDefault() {
    var path = "/str-list-d";
    // Correct value
    checkResult(path, "x[]%3D=ab&x[]%3D=cd&x[]%3D=ef", okContains("ab,cd,ef"));
    checkResult(path, "x[]%3D=πε&x[]%3D=%CE%B5%CF%80&x[]%3D=", okContains("πε,επ,empty"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("abc,def,ghi"));
    checkResult(path, "x[]%3D", okContains("empty"));
    checkResult(path, "x[]%3D=", okContains("empty"));
    // Reverse route
    assertThat(StringController.queryListDefault(List.of()).url()).isEqualTo(path);
    assertThat(StringController.queryListDefault(defaultList).url()).isEqualTo(path);
    assertThat(StringController.queryListDefault(testList).url())
        .isEqualTo(path + "?x%5B%5D%3D=ab&x%5B%5D%3D=cd&x%5B%5D%3D=ef");
    assertThat(StringController.queryListDefault(unicodeList).url())
        .isEqualTo(path + "?x%5B%5D%3D=%CF%80%CE%B5&x%5B%5D%3D=%CE%B5%CF%80");
  }

  /** Tests route {@code /str-list-null} {@link StringController#queryListNullable}. */
  @Test
  public void checkQueryListNullable() {
    var path = "/str-list-null";
    // Correct value
    checkResult(path, "x[]?=ab&x[]?=cd&x[]?=ef", okContains("ab,cd,ef"));
    checkResult(path, "x[]?=πε&x[]?=%CE%B5%CF%80&x[]?=", okContains("πε,επ,empty"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x[]?", okContains("empty"));
    checkResult(path, "x[]?=", okContains("empty"));
    // Reverse route
    assertThat(StringController.queryListNullable(null).url()).isEqualTo(path);
    assertThat(StringController.queryListNullable(List.of()).url()).isEqualTo(path);
    assertThat(StringController.queryListNullable(testList).url())
        .isEqualTo(path + "?x%5B%5D%3F=ab&x%5B%5D%3F=cd&x%5B%5D%3F=ef");
    assertThat(StringController.queryListNullable(unicodeList).url())
        .isEqualTo(path + "?x%5B%5D%3F=%CF%80%CE%B5&x%5B%5D%3F=%CE%B5%CF%80");
  }

  /** Tests route {@code /str-list-opt} {@link StringController#queryListOptional}. */
  @Test
  public void checkQueryListOptional() {
    var path = "/str-list-opt";
    // Correct value
    checkResult(path, "x[]?=ab&x[]?=cd&x[]?=ef", okContains("ab,cd,ef"));
    checkResult(path, "x[]?=πε&x[]?=%CE%B5%CF%80&x[]?=", okContains("πε,επ,empty"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]?", okContains("empty"));
    checkResult(path, "x[]?=", okContains("empty"));
    // Reverse route
    assertThat(StringController.queryListOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(StringController.queryListOptional(Optional.of(List.of())).url()).isEqualTo(path);
    assertThat(StringController.queryListOptional(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F=ab&x%5B%5D%3F=cd&x%5B%5D%3F=ef");
    assertThat(StringController.queryListOptional(Optional.of(unicodeList)).url())
        .isEqualTo(path + "?x%5B%5D%3F=%CF%80%CE%B5&x%5B%5D%3F=%CE%B5%CF%80");
  }

  /** Tests route {@code /str-list-opt-d} {@link StringController#queryListOptionalDefault}. */
  @Test
  public void checkQueryListOptionalDefault() {
    var path = "/str-list-opt-d";
    // Correct value
    checkResult(path, "x[]?%3D=ab&x[]?%3D=cd&x[]?%3D=ef", okContains("ab,cd,ef"));
    checkResult(path, "x[]?%3D=πε&x[]?%3D=%CE%B5%CF%80&x[]?%3D", okContains("πε,επ,empty"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("abc,def,ghi"));
    checkResult(path, "x[]?%3D", okContains("empty"));
    checkResult(path, "x[]?%3D=", okContains("empty"));
    // Reverse route
    assertThat(StringController.queryListOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(StringController.queryListOptionalDefault(Optional.of(List.of())).url())
        .isEqualTo(path);
    assertThat(StringController.queryListOptionalDefault(Optional.of(defaultList)).url())
        .isEqualTo(path);
    assertThat(StringController.queryListOptionalDefault(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F%3D=ab&x%5B%5D%3F%3D=cd&x%5B%5D%3F%3D=ef");
    assertThat(StringController.queryListOptionalDefault(Optional.of(unicodeList)).url())
        .isEqualTo(path + "?x%5B%5D%3F%3D=%CF%80%CE%B5&x%5B%5D%3F%3D=%CE%B5%CF%80");
  }
}
