/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static controllers.routes.FloatController;
import static java.lang.Float.NEGATIVE_INFINITY;
import static java.lang.Float.NaN;
import static java.lang.Float.POSITIVE_INFINITY;
import static org.assertj.core.api.Assertions.assertThat;

import controllers.FloatController;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class FloatParamTest extends AbstractRoutesTest {

  private static final List<Float> defaultList = List.of(1.1f, 2.2f, 3.3f);
  private static final List<Float> testList = List.of(7.7f, -8.8f, 9.9E-10f);
  private static final List<Float> infinityList =
      List.of(NaN, POSITIVE_INFINITY, NEGATIVE_INFINITY);

  /** Tests route {@code /float-p} {@link FloatController#path}. */
  @Test
  public void checkPath() {
    var path = "/float-p";
    // Correct value
    checkResult(path + "/7.89d", okContains("7.89"));
    checkResult(path + "/-7.89f", okContains("-7.89"));
    checkResult(path + "/7.8E9", okContains("7.8E9"));
    checkResult(path + "/7.8E%2B9", okContains("7.8E9"));
    checkResult(path + "/7.8E-9", okContains("7.8E-9"));
    checkResult(path + "/NaN", okContains("NaN"));
    checkResult(path + "/Infinity", okContains("Infinity"));
    checkResult(path + "/-Infinity", okContains("-Infinity"));
    checkResult(path + "/2E310", okContains("Infinity"));
    // Without param
    checkResult(path, this::notFound);
    // Incorrect value
    checkResult(path + "/invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(FloatController.path(null).url()).isEqualTo(path + "/0.0");
    assertThat(FloatController.path(7.89f).url()).isEqualTo(path + "/7.89");
    assertThat(FloatController.path(-7.89f).url()).isEqualTo(path + "/-7.89");
    assertThat(FloatController.path(NaN).url()).isEqualTo(path + "/NaN");
    assertThat(FloatController.path(POSITIVE_INFINITY).url()).isEqualTo(path + "/Infinity");
    assertThat(FloatController.path(NEGATIVE_INFINITY).url()).isEqualTo(path + "/-Infinity");
  }

  /** Tests route {@code /float} {@link FloatController#query}. */
  @Test
  public void checkQuery() {
    var path = "/float";
    // Correct value
    checkResult(path, "x=7.89", okContains("7.89"));
    checkResult(path, "x=-7.89", okContains("-7.89"));
    checkResult(path, "x=7.8E9", okContains("7.8E9"));
    checkResult(path, "x=7.8E%2B9", okContains("7.8E9"));
    checkResult(path, "x=7.8E-9", okContains("7.8E-9"));
    checkResult(path, "x=NaN", okContains("NaN"));
    checkResult(path, "x=Infinity", okContains("Infinity"));
    checkResult(path, "x=-Infinity", okContains("-Infinity"));
    checkResult(path, "x=2E310", okContains("Infinity"));
    // Without/Empty/NoValue param
    checkResult(path, this::badRequestMissingParameter);
    checkResult(path, "x", this::badRequestMissingParameter);
    checkResult(path, "x=", this::badRequestMissingParameter);
    // Incorrect value
    checkResult(path, "x=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(FloatController.query(null).url()).isEqualTo(path + "?x=0.0");
    assertThat(FloatController.query(7.89f).url()).isEqualTo(path + "?x=7.89");
    assertThat(FloatController.query(-7.89f).url()).isEqualTo(path + "?x=-7.89");
    assertThat(FloatController.query(7.8E9f).url()).isEqualTo(path + "?x=7.8E9");
    assertThat(FloatController.query(NaN).url()).isEqualTo(path + "?x=NaN");
    assertThat(FloatController.query(POSITIVE_INFINITY).url()).isEqualTo(path + "?x=Infinity");
    assertThat(FloatController.query(NEGATIVE_INFINITY).url()).isEqualTo(path + "?x=-Infinity");
  }

  /** Tests route {@code /float-d} {@link FloatController#queryDefault}. */
  @Test
  public void checkQueryDefault() {
    var path = "/float-d";
    // Correct value
    checkResult(path, "x?%3D=7.89", okContains("7.89"));
    checkResult(path, "x?%3D=-7.89", okContains("-7.89"));
    checkResult(path, "x?%3D=7.8E9", okContains("7.8E9"));
    checkResult(path, "x?%3D=NaN", okContains("NaN"));
    checkResult(path, "x?%3D=Infinity", okContains("Infinity"));
    checkResult(path, "x?%3D=-Infinity", okContains("-Infinity"));
    checkResult(path, "x?%3D=2E310", okContains("Infinity"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("1.23"));
    checkResult(path, "x?%3D", okContains("1.23"));
    checkResult(path, "x?%3D=", okContains("1.23"));
    // Incorrect value
    checkResult(path, "x?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(FloatController.queryDefault(null).url()).isEqualTo(path + "?x%3F%3D=0.0");
    assertThat(FloatController.queryDefault(1.23f).url()).isEqualTo(path);
    assertThat(FloatController.queryDefault(7.89f).url()).isEqualTo(path + "?x%3F%3D=7.89");
    assertThat(FloatController.queryDefault(-7.89f).url()).isEqualTo(path + "?x%3F%3D=-7.89");
    assertThat(FloatController.queryDefault(NaN).url()).isEqualTo(path + "?x%3F%3D=NaN");
    assertThat(FloatController.queryDefault(POSITIVE_INFINITY).url())
        .isEqualTo(path + "?x%3F%3D=Infinity");
    assertThat(FloatController.queryDefault(NEGATIVE_INFINITY).url())
        .isEqualTo(path + "?x%3F%3D=-Infinity");
  }

  /** Tests route {@code /float-f} {@link FloatController#queryFixed}. */
  @Test
  public void checkQueryFixed() {
    var path = "/float-f";
    // Correct value
    checkResult(path, "x=7.89", okContains("1.23"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("1.23"));
    checkResult(path, "x", okContains("1.23"));
    checkResult(path, "x=", okContains("1.23"));
    // Incorrect value
    checkResult(path, "x=invalid", okContains("1.23"));
    // Reverse route
    assertThat(FloatController.queryFixed().url()).isEqualTo(path);
  }

  /** Tests route {@code /float-null} {@link FloatController#queryNullable}. */
  @Test
  public void checkQueryNullable() {
    var path = "/float-null";
    // Correct value
    checkResult(path, "x?=7.89", okContains("7.89"));
    checkResult(path, "x?=-7.89", okContains("-7.89"));
    checkResult(path, "x?=7.8E9", okContains("7.8E9"));
    checkResult(path, "x?=NaN", okContains("NaN"));
    checkResult(path, "x?=Infinity", okContains("Infinity"));
    checkResult(path, "x?=-Infinity", okContains("-Infinity"));
    checkResult(path, "x?=2E310", okContains("Infinity"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x?", okContains("null"));
    checkResult(path, "x?=", okContains("null"));
    // Incorrect value
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(FloatController.queryNullable(null).url()).isEqualTo(path);
    assertThat(FloatController.queryNullable(7.89f).url()).isEqualTo(path + "?x%3F=7.89");
    assertThat(FloatController.queryNullable(-7.89f).url()).isEqualTo(path + "?x%3F=-7.89");
    assertThat(FloatController.queryNullable(NaN).url()).isEqualTo(path + "?x%3F=NaN");
    assertThat(FloatController.queryNullable(POSITIVE_INFINITY).url())
        .isEqualTo(path + "?x%3F=Infinity");
    assertThat(FloatController.queryNullable(NEGATIVE_INFINITY).url())
        .isEqualTo(path + "?x%3F=-Infinity");
  }

  /** Tests route {@code /float-opt} {@link FloatController#queryOptional}. */
  @Test
  public void checkQueryOptional() {
    var path = "/float-opt";
    // Correct value
    checkResult(path, "x?=7.89", okContains("7.89"));
    checkResult(path, "x?=-7.89", okContains("-7.89"));
    checkResult(path, "x?=7.8E9", okContains("7.8E9"));
    checkResult(path, "x?=NaN", okContains("NaN"));
    checkResult(path, "x?=Infinity", okContains("Infinity"));
    checkResult(path, "x?=-Infinity", okContains("-Infinity"));
    checkResult(path, "x?=2E310", okContains("Infinity"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmptyOptional);
    checkResult(path, "x?", this::okEmptyOptional);
    checkResult(path, "x?=", this::okEmptyOptional);
    // Incorrect value
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(FloatController.queryOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(FloatController.queryOptional(Optional.of(7.89f)).url())
        .isEqualTo(path + "?x%3F=7.89");
    assertThat(FloatController.queryOptional(Optional.of(-7.89f)).url())
        .isEqualTo(path + "?x%3F=-7.89");
    assertThat(FloatController.queryOptional(Optional.of(NaN)).url())
        .isEqualTo(path + "?x%3F=NaN");
    assertThat(FloatController.queryOptional(Optional.of(POSITIVE_INFINITY)).url())
        .isEqualTo(path + "?x%3F=Infinity");
    assertThat(FloatController.queryOptional(Optional.of(NEGATIVE_INFINITY)).url())
        .isEqualTo(path + "?x%3F=-Infinity");
  }

  /** Tests route {@code /float-opt-d} {@link FloatController#queryOptionalDefault}. */
  @Test
  public void checkQueryOptionalDefault() {
    var path = "/float-opt-d";
    // Correct value
    checkResult(path, "x?%3D=7.89", okContains("7.89"));
    checkResult(path, "x?%3D=-7.89", okContains("-7.89"));
    checkResult(path, "x?%3D=7.8E9", okContains("7.8E9"));
    checkResult(path, "x?%3D=NaN", okContains("NaN"));
    checkResult(path, "x?%3D=Infinity", okContains("Infinity"));
    checkResult(path, "x?%3D=-Infinity", okContains("-Infinity"));
    checkResult(path, "x?%3D=2E310", okContains("Infinity"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("1.23"));
    checkResult(path, "x?%3D", okContains("1.23"));
    checkResult(path, "x?%3D=", okContains("1.23"));
    // Incorrect value
    checkResult(path, "x?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(FloatController.queryOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(FloatController.queryOptionalDefault(Optional.of(1.23f)).url()).isEqualTo(path);
    assertThat(FloatController.queryOptionalDefault(Optional.of(7.89f)).url())
        .isEqualTo(path + "?x%3F%3D=7.89");
    assertThat(FloatController.queryOptionalDefault(Optional.of(-7.89f)).url())
        .isEqualTo(path + "?x%3F%3D=-7.89");
    assertThat(FloatController.queryOptionalDefault(Optional.of(NaN)).url())
        .isEqualTo(path + "?x%3F%3D=NaN");
    assertThat(FloatController.queryOptionalDefault(Optional.of(POSITIVE_INFINITY)).url())
        .isEqualTo(path + "?x%3F%3D=Infinity");
    assertThat(FloatController.queryOptionalDefault(Optional.of(NEGATIVE_INFINITY)).url())
        .isEqualTo(path + "?x%3F%3D=-Infinity");
  }

  /** Tests route {@code /float-list} {@link FloatController#queryList}. */
  @Test
  public void checkQueryList() {
    var path = "/float-list";
    // Correct value
    checkResult(path, "x[]=7.7&x[]=-8.8&x[]=9.9E-10&x[]=", okContains("7.7,-8.8,9.9E-10"));
    checkResult(
        path, "x[]=NaN&x[]=Infinity&x[]=-Infinity&x[]=", okContains("NaN,Infinity,-Infinity"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]", this::okEmpty);
    checkResult(path, "x[]=", this::okEmpty);
    // Incorrect value
    checkResult(path, "x[]=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(FloatController.queryList(List.of()).url()).isEqualTo(path);
    assertThat(FloatController.queryList(testList).url())
        .isEqualTo(path + "?x%5B%5D=7.7&x%5B%5D=-8.8&x%5B%5D=9.9E-10");
    assertThat(FloatController.queryList(infinityList).url())
        .isEqualTo(path + "?x%5B%5D=NaN&x%5B%5D=Infinity&x%5B%5D=-Infinity");
  }

  /** Tests route {@code /float-list-d} {@link FloatController#queryListDefault}. */
  @Test
  public void checkQueryListDefault() {
    var path = "/float-list-d";
    // Correct value
    checkResult(
        path, "x[]%3D=7.7&x[]%3D=-8.8&x[]%3D=9.9E-10&x[]%3D=", okContains("7.7,-8.8,9.9E-10"));
    checkResult(
        path,
        "x[]%3D=NaN&x[]%3D=Infinity&x[]%3D=-Infinity&x[]%3D=",
        okContains("NaN,Infinity,-Infinity"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("1.1,2.2,3.3"));
    checkResult(path, "x[]%3D", okContains("1.1,2.2,3.3"));
    checkResult(path, "x[]%3D=", okContains("1.1,2.2,3.3"));
    // Incorrect value
    checkResult(path, "x[]%3D=1&x[]%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(FloatController.queryListDefault(List.of()).url()).isEqualTo(path);
    assertThat(FloatController.queryListDefault(defaultList).url()).isEqualTo(path);
    assertThat(FloatController.queryListDefault(testList).url())
        .isEqualTo(path + "?x%5B%5D%3D=7.7&x%5B%5D%3D=-8.8&x%5B%5D%3D=9.9E-10");
    assertThat(FloatController.queryListDefault(infinityList).url())
        .isEqualTo(path + "?x%5B%5D%3D=NaN&x%5B%5D%3D=Infinity&x%5B%5D%3D=-Infinity");
  }

  /** Tests route {@code /float-list-null} {@link FloatController#queryListNullable}. */
  @Test
  public void checkQueryListNullable() {
    var path = "/float-list-null";
    // Correct value
    checkResult(path, "x[]?=7.7&x[]?=-8.8&x[]?=9.9E-10&x[]?=", okContains("7.7,-8.8,9.9E-10"));
    checkResult(
        path, "x[]?=NaN&x[]?=Infinity&x[]?=-Infinity&x[]?=", okContains("NaN,Infinity,-Infinity"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x[]?", okContains("null"));
    checkResult(path, "x[]?=", okContains("null"));
    // Incorrect value
    checkResult(path, "x[]?=1&x[]?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(FloatController.queryListNullable(null).url()).isEqualTo(path);
    assertThat(FloatController.queryListNullable(List.of()).url()).isEqualTo(path);
    assertThat(FloatController.queryListNullable(testList).url())
        .isEqualTo(path + "?x%5B%5D%3F=7.7&x%5B%5D%3F=-8.8&x%5B%5D%3F=9.9E-10");
    assertThat(FloatController.queryListNullable(infinityList).url())
        .isEqualTo(path + "?x%5B%5D%3F=NaN&x%5B%5D%3F=Infinity&x%5B%5D%3F=-Infinity");
  }

  /** Tests route {@code /float-list-opt} {@link FloatController#queryListOptional}. */
  @Test
  public void checkQueryListOptional() {
    var path = "/float-list-opt";
    // Correct value
    checkResult(path, "x[]?=7.7&x[]?=-8.8&x[]?=9.9E-10&x[]?=", okContains("7.7,-8.8,9.9E-10"));
    checkResult(
        path, "x[]?=NaN&x[]?=Infinity&x[]?=-Infinity", okContains("NaN,Infinity,-Infinity"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]?", this::okEmpty);
    checkResult(path, "x[]?=", this::okEmpty);
    // Incorrect value
    checkResult(path, "x[]?=1&x[]?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(FloatController.queryListOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(FloatController.queryListOptional(Optional.of(List.of())).url()).isEqualTo(path);
    assertThat(FloatController.queryListOptional(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F=7.7&x%5B%5D%3F=-8.8&x%5B%5D%3F=9.9E-10");
    assertThat(FloatController.queryListOptional(Optional.of(infinityList)).url())
        .isEqualTo(path + "?x%5B%5D%3F=NaN&x%5B%5D%3F=Infinity&x%5B%5D%3F=-Infinity");
  }

  /** Tests route {@code /float-list-opt-d} {@link FloatController#queryListOptionalDefault}. */
  @Test
  public void checkQueryListOptionalDefault() {
    var path = "/float-list-opt-d";
    // Correct value
    checkResult(
        path, "x[]?%3D=7.7&x[]?%3D=-8.8&x[]?%3D=9.9E-10&x[]?%3D=", okContains("7.7,-8.8,9.9E-10"));
    checkResult(
        path,
        "x[]?%3D=NaN&x[]?%3D=Infinity&x[]?%3D=-Infinity&x[]?%3D=",
        okContains("NaN,Infinity,-Infinity"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("1.1,2.2,3.3"));
    checkResult(path, "x[]?%3D", okContains("1.1,2.2,3.3"));
    checkResult(path, "x[]?%3D=", okContains("1.1,2.2,3.3"));
    // Incorrect value
    checkResult(path, "x[]?%3D=1&x[]?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(FloatController.queryListOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(FloatController.queryListOptionalDefault(Optional.of(List.of())).url())
        .isEqualTo(path);
    assertThat(FloatController.queryListOptionalDefault(Optional.of(defaultList)).url())
        .isEqualTo(path);
    assertThat(FloatController.queryListOptionalDefault(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F%3D=7.7&x%5B%5D%3F%3D=-8.8&x%5B%5D%3F%3D=9.9E-10");
    assertThat(FloatController.queryListOptionalDefault(Optional.of(infinityList)).url())
        .isEqualTo(path + "?x%5B%5D%3F%3D=NaN&x%5B%5D%3F%3D=Infinity&x%5B%5D%3F%3D=-Infinity");
  }
}
