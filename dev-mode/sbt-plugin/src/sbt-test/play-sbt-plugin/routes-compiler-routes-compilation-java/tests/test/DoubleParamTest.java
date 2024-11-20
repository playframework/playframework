/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static controllers.routes.DoubleController;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.NaN;
import static java.lang.Double.POSITIVE_INFINITY;
import static org.assertj.core.api.Assertions.assertThat;

import controllers.DoubleController;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class DoubleParamTest extends AbstractRoutesTest {

  private static final List<Double> defaultList = List.of(1.1, 2.2, 3.3);
  private static final List<Double> testList = List.of(7.7, -8.8, 9.9E-10);
  private static final List<Double> infinityList =
      List.of(NaN, POSITIVE_INFINITY, NEGATIVE_INFINITY);

  /** Tests route {@code /double-p} {@link DoubleController#path}. */
  @Test
  public void checkPath() {
    var path = "/double-p";
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
    assertThat(DoubleController.path(null).url()).isEqualTo(path + "/0.0");
    assertThat(DoubleController.path(7.89).url()).isEqualTo(path + "/7.89");
    assertThat(DoubleController.path(-7.89).url()).isEqualTo(path + "/-7.89");
    assertThat(DoubleController.path(NaN).url()).isEqualTo(path + "/NaN");
    assertThat(DoubleController.path(POSITIVE_INFINITY).url()).isEqualTo(path + "/Infinity");
    assertThat(DoubleController.path(NEGATIVE_INFINITY).url()).isEqualTo(path + "/-Infinity");
  }

  /** Tests route {@code /double} {@link DoubleController#query}. */
  @Test
  public void checkQuery() {
    var path = "/double";
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
    assertThat(DoubleController.query(null).url()).isEqualTo(path + "?x=0.0");
    assertThat(DoubleController.query(7.89).url()).isEqualTo(path + "?x=7.89");
    assertThat(DoubleController.query(-7.89).url()).isEqualTo(path + "?x=-7.89");
    assertThat(DoubleController.query(7.8E9).url()).isEqualTo(path + "?x=7.8E9");
    assertThat(DoubleController.query(NaN).url()).isEqualTo(path + "?x=NaN");
    assertThat(DoubleController.query(POSITIVE_INFINITY).url()).isEqualTo(path + "?x=Infinity");
    assertThat(DoubleController.query(NEGATIVE_INFINITY).url()).isEqualTo(path + "?x=-Infinity");
  }

  /** Tests route {@code /double-d} {@link DoubleController#queryDefault}. */
  @Test
  public void checkQueryDefault() {
    var path = "/double-d";
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
    assertThat(DoubleController.queryDefault(null).url()).isEqualTo(path + "?x%3F%3D=0.0");
    assertThat(DoubleController.queryDefault(1.23).url()).isEqualTo(path);
    assertThat(DoubleController.queryDefault(7.89).url()).isEqualTo(path + "?x%3F%3D=7.89");
    assertThat(DoubleController.queryDefault(-7.89).url()).isEqualTo(path + "?x%3F%3D=-7.89");
    assertThat(DoubleController.queryDefault(NaN).url()).isEqualTo(path + "?x%3F%3D=NaN");
    assertThat(DoubleController.queryDefault(POSITIVE_INFINITY).url())
        .isEqualTo(path + "?x%3F%3D=Infinity");
    assertThat(DoubleController.queryDefault(NEGATIVE_INFINITY).url())
        .isEqualTo(path + "?x%3F%3D=-Infinity");
  }

  /** Tests route {@code /double-f} {@link DoubleController#queryFixed}. */
  @Test
  public void checkQueryFixed() {
    var path = "/double-f";
    // Correct value
    checkResult(path, "x=7.89", okContains("1.23"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("1.23"));
    checkResult(path, "x", okContains("1.23"));
    checkResult(path, "x=", okContains("1.23"));
    // Incorrect value
    checkResult(path, "x=invalid", okContains("1.23"));
    // Reverse route
    assertThat(DoubleController.queryFixed().url()).isEqualTo(path);
  }

  /** Tests route {@code /double-null} {@link DoubleController#queryNullable}. */
  @Test
  public void checkQueryNullable() {
    var path = "/double-null";
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
    assertThat(DoubleController.queryNullable(null).url()).isEqualTo(path);
    assertThat(DoubleController.queryNullable(7.89).url()).isEqualTo(path + "?x%3F=7.89");
    assertThat(DoubleController.queryNullable(-7.89).url()).isEqualTo(path + "?x%3F=-7.89");
    assertThat(DoubleController.queryNullable(NaN).url()).isEqualTo(path + "?x%3F=NaN");
    assertThat(DoubleController.queryNullable(POSITIVE_INFINITY).url())
        .isEqualTo(path + "?x%3F=Infinity");
    assertThat(DoubleController.queryNullable(NEGATIVE_INFINITY).url())
        .isEqualTo(path + "?x%3F=-Infinity");
  }

  /** Tests route {@code /double-opt} {@link DoubleController#queryOptional}. */
  @Test
  public void checkQueryOptional() {
    var path = "/double-opt";
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
    assertThat(DoubleController.queryOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(DoubleController.queryOptional(Optional.of(7.89)).url())
        .isEqualTo(path + "?x%3F=7.89");
    assertThat(DoubleController.queryOptional(Optional.of(-7.89)).url())
        .isEqualTo(path + "?x%3F=-7.89");
    assertThat(DoubleController.queryOptional(Optional.of(NaN)).url())
        .isEqualTo(path + "?x%3F=NaN");
    assertThat(DoubleController.queryOptional(Optional.of(POSITIVE_INFINITY)).url())
        .isEqualTo(path + "?x%3F=Infinity");
    assertThat(DoubleController.queryOptional(Optional.of(NEGATIVE_INFINITY)).url())
        .isEqualTo(path + "?x%3F=-Infinity");
  }

  /** Tests route {@code /double-opt-d} {@link DoubleController#queryOptionalDefault}. */
  @Test
  public void checkQueryOptionalDefault() {
    var path = "/double-opt-d";
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
    assertThat(DoubleController.queryOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(DoubleController.queryOptionalDefault(Optional.of(1.23)).url()).isEqualTo(path);
    assertThat(DoubleController.queryOptionalDefault(Optional.of(7.89)).url())
        .isEqualTo(path + "?x%3F%3D=7.89");
    assertThat(DoubleController.queryOptionalDefault(Optional.of(-7.89)).url())
        .isEqualTo(path + "?x%3F%3D=-7.89");
    assertThat(DoubleController.queryOptionalDefault(Optional.of(NaN)).url())
        .isEqualTo(path + "?x%3F%3D=NaN");
    assertThat(DoubleController.queryOptionalDefault(Optional.of(POSITIVE_INFINITY)).url())
        .isEqualTo(path + "?x%3F%3D=Infinity");
    assertThat(DoubleController.queryOptionalDefault(Optional.of(NEGATIVE_INFINITY)).url())
        .isEqualTo(path + "?x%3F%3D=-Infinity");
  }

  /** Tests route {@code /double-list} {@link DoubleController#queryList}. */
  @Test
  public void checkQueryList() {
    var path = "/double-list";
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
    assertThat(DoubleController.queryList(List.of()).url()).isEqualTo(path);
    assertThat(DoubleController.queryList(testList).url())
        .isEqualTo(path + "?x%5B%5D=7.7&x%5B%5D=-8.8&x%5B%5D=9.9E-10");
    assertThat(DoubleController.queryList(infinityList).url())
        .isEqualTo(path + "?x%5B%5D=NaN&x%5B%5D=Infinity&x%5B%5D=-Infinity");
  }

  /** Tests route {@code /double-list-d} {@link DoubleController#queryListDefault}. */
  @Test
  public void checkQueryListDefault() {
    var path = "/double-list-d";
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
    assertThat(DoubleController.queryListDefault(List.of()).url()).isEqualTo(path);
    assertThat(DoubleController.queryListDefault(defaultList).url()).isEqualTo(path);
    assertThat(DoubleController.queryListDefault(testList).url())
        .isEqualTo(path + "?x%5B%5D%3D=7.7&x%5B%5D%3D=-8.8&x%5B%5D%3D=9.9E-10");
    assertThat(DoubleController.queryListDefault(infinityList).url())
        .isEqualTo(path + "?x%5B%5D%3D=NaN&x%5B%5D%3D=Infinity&x%5B%5D%3D=-Infinity");
  }

  /** Tests route {@code /double-list-null} {@link DoubleController#queryListNullable}. */
  @Test
  public void checkQueryListNullable() {
    var path = "/double-list-null";
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
    assertThat(DoubleController.queryListNullable(null).url()).isEqualTo(path);
    assertThat(DoubleController.queryListNullable(List.of()).url()).isEqualTo(path);
    assertThat(DoubleController.queryListNullable(testList).url())
        .isEqualTo(path + "?x%5B%5D%3F=7.7&x%5B%5D%3F=-8.8&x%5B%5D%3F=9.9E-10");
    assertThat(DoubleController.queryListNullable(infinityList).url())
        .isEqualTo(path + "?x%5B%5D%3F=NaN&x%5B%5D%3F=Infinity&x%5B%5D%3F=-Infinity");
  }

  /** Tests route {@code /double-list-opt} {@link DoubleController#queryListOptional}. */
  @Test
  public void checkQueryListOptional() {
    var path = "/double-list-opt";
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
    assertThat(DoubleController.queryListOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(DoubleController.queryListOptional(Optional.of(List.of())).url()).isEqualTo(path);
    assertThat(DoubleController.queryListOptional(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F=7.7&x%5B%5D%3F=-8.8&x%5B%5D%3F=9.9E-10");
    assertThat(DoubleController.queryListOptional(Optional.of(infinityList)).url())
        .isEqualTo(path + "?x%5B%5D%3F=NaN&x%5B%5D%3F=Infinity&x%5B%5D%3F=-Infinity");
  }

  /** Tests route {@code /double-list-opt-d} {@link DoubleController#queryListOptionalDefault}. */
  @Test
  public void checkQueryListOptionalDefault() {
    var path = "/double-list-opt-d";
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
    assertThat(DoubleController.queryListOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(DoubleController.queryListOptionalDefault(Optional.of(List.of())).url())
        .isEqualTo(path);
    assertThat(DoubleController.queryListOptionalDefault(Optional.of(defaultList)).url())
        .isEqualTo(path);
    assertThat(DoubleController.queryListOptionalDefault(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F%3D=7.7&x%5B%5D%3F%3D=-8.8&x%5B%5D%3F%3D=9.9E-10");
    assertThat(DoubleController.queryListOptionalDefault(Optional.of(infinityList)).url())
        .isEqualTo(path + "?x%5B%5D%3F%3D=NaN&x%5B%5D%3F%3D=Infinity&x%5B%5D%3F%3D=-Infinity");
  }
}
