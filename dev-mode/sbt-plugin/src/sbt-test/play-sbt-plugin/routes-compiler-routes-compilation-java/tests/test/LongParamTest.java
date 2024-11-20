/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static controllers.routes.LongController;
import static org.assertj.core.api.Assertions.assertThat;

import controllers.LongController;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class LongParamTest extends AbstractRoutesTest {

  private static final List<Long> defaultList = List.of(1L, 2L, 3L);
  private static final List<Long> testList = List.of(7L, -8L, 3_000_000_000L);

  /** Tests route {@code /long-p} {@link LongController#path}. */
  @Test
  public void checkPath() {
    var path = "/long-p";
    // Correct value
    checkResult(path + "/789", okContains("789"));
    checkResult(path + "/-789", okContains("-789"));
    // Without param
    checkResult(path, this::notFound);
    // Incorrect value
    checkResult(
        path + "/10000000000000000000", this::badRequestCannotParseParameter); // 10^19 > 2^63
    checkResult(path + "/1.0", this::badRequestCannotParseParameter);
    checkResult(path + "/invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(LongController.path(null).url()).isEqualTo(path + "/0");
    assertThat(LongController.path(789L).url()).isEqualTo(path + "/789");
    assertThat(LongController.path(-789L).url()).isEqualTo(path + "/-789");
  }

  /** Tests route {@code /long} {@link LongController#query}. */
  @Test
  public void checkQuery() {
    var path = "/long";
    // Correct value
    checkResult(path, "x=789", okContains("789"));
    checkResult(path, "x=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, this::badRequestMissingParameter);
    checkResult(path, "x", this::badRequestMissingParameter);
    checkResult(path, "x=", this::badRequestMissingParameter);
    // Incorrect value
    checkResult(
        path, "x=10000000000000000000", this::badRequestCannotParseParameter); // 10^19 > 2^63
    checkResult(path, "x=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(LongController.query(null).url()).isEqualTo(path + "?x=0");
    assertThat(LongController.query(789L).url()).isEqualTo(path + "?x=789");
    assertThat(LongController.query(-789L).url()).isEqualTo(path + "?x=-789");
  }

  /** Tests route {@code /long-d} {@link LongController#queryDefault}. */
  @Test
  public void checkQueryDefault() {
    var path = "/long-d";
    // Correct value
    checkResult(path, "x?%3D=789", okContains("789"));
    checkResult(path, "x?%3D=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("123"));
    checkResult(path, "x?%3D", okContains("123"));
    checkResult(path, "x?%3D=", okContains("123"));
    // Incorrect value
    checkResult(
        path, "x?%3D=10000000000000000000", this::badRequestCannotParseParameter); // 10^19 > 2^63
    checkResult(path, "x?%3D=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(LongController.queryDefault(null).url()).isEqualTo(path + "?x%3F%3D=0");
    assertThat(LongController.queryDefault(123L).url()).isEqualTo(path);
    assertThat(LongController.queryDefault(789L).url()).isEqualTo(path + "?x%3F%3D=789");
    assertThat(LongController.queryDefault(-789L).url()).isEqualTo(path + "?x%3F%3D=-789");
  }

  /** Tests route {@code /long-f} {@link LongController#queryFixed}. */
  @Test
  public void checkQueryFixed() {
    var path = "/long-f";
    // Correct value
    checkResult(path, "x=789", okContains("123"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("123"));
    checkResult(path, "x", okContains("123"));
    checkResult(path, "x=", okContains("123"));
    // Incorrect value
    checkResult(path, "x=invalid", okContains("123"));
    // Reverse route
    assertThat(LongController.queryFixed().url()).isEqualTo(path);
  }

  /** Tests route {@code /long-null} {@link LongController#queryNullable}. */
  @Test
  public void checkQueryNullable() {
    var path = "/long-null";
    // Correct value
    checkResult(path, "x?=789", okContains("789"));
    checkResult(path, "x?=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x?", okContains("null"));
    checkResult(path, "x?=", okContains("null"));
    // Incorrect value
    checkResult(
        path, "x?=10000000000000000000", this::badRequestCannotParseParameter); // 10^19 > 2^63
    checkResult(path, "x?=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(LongController.queryNullable(null).url()).isEqualTo(path);
    assertThat(LongController.queryNullable(789L).url()).isEqualTo(path + "?x%3F=789");
    assertThat(LongController.queryNullable(-789L).url()).isEqualTo(path + "?x%3F=-789");
  }

  /** Tests route {@code /long-opt} {@link LongController#queryOptional}. */
  @Test
  public void checkQueryOptional() {
    var path = "/long-opt";
    // Correct value
    checkResult(path, "x?=789", okContains("789"));
    checkResult(path, "x?=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmptyOptional);
    checkResult(path, "x?", this::okEmptyOptional);
    checkResult(path, "x?=", this::okEmptyOptional);
    // Incorrect value
    checkResult(
        path, "x?=10000000000000000000", this::badRequestCannotParseParameter); // 10^19 > 2^63
    checkResult(path, "x?=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(LongController.queryOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(LongController.queryOptional(Optional.of(789L)).url()).isEqualTo(path + "?x%3F=789");
    assertThat(LongController.queryOptional(Optional.of(-789L)).url())
        .isEqualTo(path + "?x%3F=-789");
  }

  /** Tests route {@code /long-opt-d} {@link LongController#queryOptionalDefault}. */
  @Test
  public void checkQueryOptionalDefault() {
    var path = "/long-opt-d";
    // Correct value
    checkResult(path, "x?%3D=789", okContains("789"));
    checkResult(path, "x?%3D=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("123"));
    checkResult(path, "x?%3D", okContains("123"));
    checkResult(path, "x?%3D=", okContains("123"));
    // Incorrect value
    checkResult(
        path, "x?%3D=10000000000000000000", this::badRequestCannotParseParameter); // 10^19 > 2^63
    checkResult(path, "x?%3D=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(LongController.queryOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(LongController.queryOptionalDefault(Optional.of(123L)).url()).isEqualTo(path);
    assertThat(LongController.queryOptionalDefault(Optional.of(789L)).url())
        .isEqualTo(path + "?x%3F%3D=789");
    assertThat(LongController.queryOptionalDefault(Optional.of(-789L)).url())
        .isEqualTo(path + "?x%3F%3D=-789");
  }

  /** Tests route {@code /long-list} {@link LongController#queryList}. */
  @Test
  public void checkQueryList() {
    var path = "/long-list";
    // Correct value
    checkResult(path, "x[]=7&x[]=-8&x[]=", okContains("7,-8"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]", this::okEmpty);
    checkResult(path, "x[]=", this::okEmpty);
    // Incorrect value
    checkResult(
        path, "x[]=10000000000000000000", this::badRequestCannotParseParameter); // 10^19 > 2^63
    checkResult(path, "x[]=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x[]=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(LongController.queryList(List.of()).url()).isEqualTo(path);
    assertThat(LongController.queryList(testList).url())
        .isEqualTo(path + "?x%5B%5D=7&x%5B%5D=-8&x%5B%5D=3000000000");
  }

  /** Tests route {@code /long-list-d} {@link LongController#queryListDefault}. */
  @Test
  public void checkQueryListDefault() {
    var path = "/long-list-d";
    // Correct value
    checkResult(path, "x[]%3D=7&x[]%3D=-8&x[]%3D=", okContains("7,-8"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("1,2,3"));
    checkResult(path, "x[]%3D", okContains("1,2,3"));
    checkResult(path, "x[]%3D=", okContains("1,2,3"));
    // Incorrect value
    checkResult(
        path,
        "x[]%3D=1&x[]%3D=10000000000000000000", // 10^19 > 2^63
        this::badRequestCannotParseParameter);
    checkResult(path, "x[]%3D=1&x[]%3D=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x[]%3D=1&x[]%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(LongController.queryListDefault(List.of()).url()).isEqualTo(path);
    assertThat(LongController.queryListDefault(defaultList).url()).isEqualTo(path);
    assertThat(LongController.queryListDefault(testList).url())
        .isEqualTo(path + "?x%5B%5D%3D=7&x%5B%5D%3D=-8&x%5B%5D%3D=3000000000");
  }

  /** Tests route {@code /long-list-null} {@link LongController#queryListNullable}. */
  @Test
  public void checkQueryListNullable() {
    var path = "/long-list-null";
    // Correct value
    checkResult(path, "x[]?=7&x[]?=-8&x[]?=", okContains("7,-8"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x[]?", okContains("null"));
    checkResult(path, "x[]?=", okContains("null"));
    // Incorrect value
    checkResult(
        path,
        "x[]?=1&x[]?=10000000000000000000", // 10^19 > 2^63
        this::badRequestCannotParseParameter);
    checkResult(path, "x[]?=1&x[]?=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x[]?=1&x[]?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(LongController.queryListNullable(null).url()).isEqualTo(path);
    assertThat(LongController.queryListNullable(List.of()).url()).isEqualTo(path);
    assertThat(LongController.queryListNullable(testList).url())
        .isEqualTo(path + "?x%5B%5D%3F=7&x%5B%5D%3F=-8&x%5B%5D%3F=3000000000");
  }

  /** Tests route {@code /long-list-opt} {@link LongController#queryListOptional}. */
  @Test
  public void checkQueryListOptional() {
    var path = "/long-list-opt";
    // Correct value
    checkResult(path, "x[]?=7&x[]?=-8&x[]?=", okContains("7,-8"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]?", this::okEmpty);
    checkResult(path, "x[]?=", this::okEmpty);
    // Incorrect value
    checkResult(
        path,
        "x[]?=1&x[]?=10000000000000000000", // 10^19 > 2^63
        this::badRequestCannotParseParameter);
    checkResult(path, "x[]?=1&x[]?=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x[]?=1&x[]?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(LongController.queryListOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(LongController.queryListOptional(Optional.of(List.of())).url()).isEqualTo(path);
    assertThat(LongController.queryListOptional(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F=7&x%5B%5D%3F=-8&x%5B%5D%3F=3000000000");
  }

  /** Tests route {@code /long-list-opt-d} {@link LongController#queryListOptionalDefault}. */
  @Test
  public void checkQueryListOptionalDefault() {
    var path = "/long-list-opt-d";
    // Correct value
    checkResult(path, "x[]?%3D=7&x[]?%3D=-8&x[]?%3D=", okContains("7,-8"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("1,2,3"));
    checkResult(path, "x[]?%3D", okContains("1,2,3"));
    checkResult(path, "x[]?%3D=", okContains("1,2,3"));
    // Incorrect value
    checkResult(
        path,
        "x[]?%3D=1&x[]?%3D=10000000000000000000", // 10^19 > 2^63
        this::badRequestCannotParseParameter);
    checkResult(path, "x[]?%3D=1&x[]?%3D=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x[]?%3D=1&x[]?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(LongController.queryListOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(LongController.queryListOptionalDefault(Optional.of(List.of())).url())
        .isEqualTo(path);
    assertThat(LongController.queryListOptionalDefault(Optional.of(defaultList)).url())
        .isEqualTo(path);
    assertThat(LongController.queryListOptionalDefault(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F%3D=7&x%5B%5D%3F%3D=-8&x%5B%5D%3F%3D=3000000000");
  }
}
