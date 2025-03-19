/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static controllers.routes.IntegerController;
import static org.assertj.core.api.Assertions.assertThat;

import controllers.IntegerController;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class IntegerParamTest extends AbstractRoutesTest {

  private static final List<Integer> defaultList = List.of(1, 2, 3);
  private static final List<Integer> testList = List.of(7, -8, 65536);

  /** Tests route {@code /int-p} {@link IntegerController#path}. */
  @Test
  public void checkPath() {
    var path = "/int-p";
    // Correct value
    checkResult(path + "/789", okContains("789"));
    checkResult(path + "/-789", okContains("-789"));
    // Without param
    checkResult(path, this::notFound);
    // Incorrect value
    checkResult(path + "/3000000000", this::badRequestCannotParseParameter);
    checkResult(path + "/1.0", this::badRequestCannotParseParameter);
    checkResult(path + "/invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(IntegerController.path(null).url()).isEqualTo(path + "/0");
    assertThat(IntegerController.path(789).url()).isEqualTo(path + "/789");
    assertThat(IntegerController.path(-789).url()).isEqualTo(path + "/-789");
  }

  /** Tests route {@code /int} {@link IntegerController#query}. */
  @Test
  public void checkQuery() {
    var path = "/int";
    // Correct value
    checkResult(path, "x=789", okContains("789"));
    checkResult(path, "x=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, this::badRequestMissingParameter);
    checkResult(path, "x", this::badRequestMissingParameter);
    checkResult(path, "x=", this::badRequestMissingParameter);
    // Incorrect value
    checkResult(path, "x=3000000000", this::badRequestCannotParseParameter);
    checkResult(path, "x=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(IntegerController.query(null).url()).isEqualTo(path + "?x=0");
    assertThat(IntegerController.query(789).url()).isEqualTo(path + "?x=789");
    assertThat(IntegerController.query(-789).url()).isEqualTo(path + "?x=-789");
  }

  /** Tests route {@code /int-d} {@link IntegerController#queryDefault}. */
  @Test
  public void checkQueryDefault() {
    var path = "/int-d";
    // Correct value
    checkResult(path, "x?%3D=789", okContains("789"));
    checkResult(path, "x?%3D=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("123"));
    checkResult(path, "x?%3D", okContains("123"));
    checkResult(path, "x?%3D=", okContains("123"));
    // Incorrect value
    checkResult(path, "x?%3D=3000000000", this::badRequestCannotParseParameter);
    checkResult(path, "x?%3D=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(IntegerController.queryDefault(null).url()).isEqualTo(path + "?x%3F%3D=0");
    assertThat(IntegerController.queryDefault(123).url()).isEqualTo(path);
    assertThat(IntegerController.queryDefault(789).url()).isEqualTo(path + "?x%3F%3D=789");
    assertThat(IntegerController.queryDefault(-789).url()).isEqualTo(path + "?x%3F%3D=-789");
  }

  /** Tests route {@code /int-f} {@link IntegerController#queryFixed}. */
  @Test
  public void checkQueryFixed() {
    var path = "/int-f";
    // Correct value
    checkResult(path, "x=789", okContains("123"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("123"));
    checkResult(path, "x", okContains("123"));
    checkResult(path, "x=", okContains("123"));
    // Incorrect value
    checkResult(path, "x=invalid", okContains("123"));
    // Reverse route
    assertThat(IntegerController.queryFixed().url()).isEqualTo(path);
  }

  /** Tests route {@code /int-null} {@link IntegerController#queryNullable}. */
  @Test
  public void checkQueryNullable() {
    var path = "/int-null";
    // Correct value
    checkResult(path, "x?=789", okContains("789"));
    checkResult(path, "x?=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x?", okContains("null"));
    checkResult(path, "x?=", okContains("null"));
    // Incorrect value
    checkResult(path, "x?=3000000000", this::badRequestCannotParseParameter);
    checkResult(path, "x?=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(IntegerController.queryNullable(null).url()).isEqualTo(path);
    assertThat(IntegerController.queryNullable(789).url()).isEqualTo(path + "?x%3F=789");
    assertThat(IntegerController.queryNullable(-789).url()).isEqualTo(path + "?x%3F=-789");
  }

  /** Tests route {@code /int-opt} {@link IntegerController#queryOptional}. */
  @Test
  public void checkQueryOptional() {
    var path = "/int-opt";
    // Correct value
    checkResult(path, "x?=789", okContains("789"));
    checkResult(path, "x?=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmptyOptional);
    checkResult(path, "x?", this::okEmptyOptional);
    checkResult(path, "x?=", this::okEmptyOptional);
    // Incorrect value
    checkResult(path, "x?=3000000000", this::badRequestCannotParseParameter);
    checkResult(path, "x?=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(IntegerController.queryOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(IntegerController.queryOptional(Optional.of(789)).url())
        .isEqualTo(path + "?x%3F=789");
    assertThat(IntegerController.queryOptional(Optional.of(-789)).url())
        .isEqualTo(path + "?x%3F=-789");
  }

  /** Tests route {@code /int-opt-d} {@link IntegerController#queryOptionalDefault}. */
  @Test
  public void checkQueryOptionalDefault() {
    var path = "/int-opt-d";
    // Correct value
    checkResult(path, "x?%3D=789", okContains("789"));
    checkResult(path, "x?%3D=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("123"));
    checkResult(path, "x?%3D", okContains("123"));
    checkResult(path, "x?%3D=", okContains("123"));
    // Incorrect value
    checkResult(path, "x?%3D=3000000000", this::badRequestCannotParseParameter);
    checkResult(path, "x?%3D=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(IntegerController.queryOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(IntegerController.queryOptionalDefault(Optional.of(123)).url()).isEqualTo(path);
    assertThat(IntegerController.queryOptionalDefault(Optional.of(789)).url())
        .isEqualTo(path + "?x%3F%3D=789");
    assertThat(IntegerController.queryOptionalDefault(Optional.of(-789)).url())
        .isEqualTo(path + "?x%3F%3D=-789");
  }

  /** Tests route {@code /int-list} {@link IntegerController#queryList}. */
  @Test
  public void checkQueryList() {
    var path = "/int-list";
    // Correct value
    checkResult(path, "x[]=7&x[]=-8&x[]=", okContains("7,-8"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]", this::okEmpty);
    checkResult(path, "x[]=", this::okEmpty);
    // Incorrect value
    checkResult(path, "x[]=3000000000", this::badRequestCannotParseParameter);
    checkResult(path, "x[]=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x[]=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(IntegerController.queryList(List.of()).url()).isEqualTo(path);
    assertThat(IntegerController.queryList(testList).url())
        .isEqualTo(path + "?x%5B%5D=7&x%5B%5D=-8&x%5B%5D=65536");
  }

  /** Tests route {@code /int-list-d} {@link IntegerController#queryListDefault}. */
  @Test
  public void checkQueryListDefault() {
    var path = "/int-list-d";
    // Correct value
    checkResult(path, "x[]%3D=7&x[]%3D=-8&x[]%3D=", okContains("7,-8"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("1,2,3"));
    checkResult(path, "x[]%3D", okContains("1,2,3"));
    checkResult(path, "x[]%3D=", okContains("1,2,3"));
    // Incorrect value
    checkResult(path, "x[]%3D=1&x[]%3D=3000000000", this::badRequestCannotParseParameter);
    checkResult(path, "x[]%3D=1&x[]%3D=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x[]%3D=1&x[]%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(IntegerController.queryListDefault(List.of()).url()).isEqualTo(path);
    assertThat(IntegerController.queryListDefault(defaultList).url()).isEqualTo(path);
    assertThat(IntegerController.queryListDefault(testList).url())
        .isEqualTo(path + "?x%5B%5D%3D=7&x%5B%5D%3D=-8&x%5B%5D%3D=65536");
  }

  /** Tests route {@code /int-list-null} {@link IntegerController#queryListNullable}. */
  @Test
  public void checkQueryListNullable() {
    var path = "/int-list-null";
    // Correct value
    checkResult(path, "x[]?=7&x[]?=-8&x[]?=", okContains("7,-8"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x[]?", okContains("null"));
    checkResult(path, "x[]?=", okContains("null"));
    // Incorrect value
    checkResult(path, "x[]?=1&x[]?=3000000000", this::badRequestCannotParseParameter);
    checkResult(path, "x[]?=1&x[]?=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x[]?=1&x[]?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(IntegerController.queryListNullable(null).url()).isEqualTo(path);
    assertThat(IntegerController.queryListNullable(List.of()).url()).isEqualTo(path);
    assertThat(IntegerController.queryListNullable(testList).url())
        .isEqualTo(path + "?x%5B%5D%3F=7&x%5B%5D%3F=-8&x%5B%5D%3F=65536");
  }

  /** Tests route {@code /int-list-opt} {@link IntegerController#queryListOptional}. */
  @Test
  public void checkQueryListOptional() {
    var path = "/int-list-opt";
    // Correct value
    checkResult(path, "x[]?=7&x[]?=-8&x[]?=", okContains("7,-8"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]?", this::okEmpty);
    checkResult(path, "x[]?=", this::okEmpty);
    // Incorrect value
    checkResult(path, "x[]?=1&x[]?=3000000000", this::badRequestCannotParseParameter);
    checkResult(path, "x[]?=1&x[]?=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x[]?=1&x[]?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(IntegerController.queryListOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(IntegerController.queryListOptional(Optional.of(List.of())).url()).isEqualTo(path);
    assertThat(IntegerController.queryListOptional(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F=7&x%5B%5D%3F=-8&x%5B%5D%3F=65536");
  }

  /** Tests route {@code /int-list-opt-d} {@link IntegerController#queryListOptionalDefault}. */
  @Test
  public void checkQueryListOptionalDefault() {
    var path = "/int-list-opt-d";
    // Correct value
    checkResult(path, "x[]?%3D=7&x[]?%3D=-8&x[]?%3D=", okContains("7,-8"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("1,2,3"));
    checkResult(path, "x[]?%3D", okContains("1,2,3"));
    checkResult(path, "x[]?%3D=", okContains("1,2,3"));
    // Incorrect value
    checkResult(path, "x[]?%3D=1&x[]?%3D=3000000000", this::badRequestCannotParseParameter);
    checkResult(path, "x[]?%3D=1&x[]?%3D=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x[]?%3D=1&x[]?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(IntegerController.queryListOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(IntegerController.queryListOptionalDefault(Optional.of(List.of())).url())
        .isEqualTo(path);
    assertThat(IntegerController.queryListOptionalDefault(Optional.of(defaultList)).url())
        .isEqualTo(path);
    assertThat(IntegerController.queryListOptionalDefault(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F%3D=7&x%5B%5D%3F%3D=-8&x%5B%5D%3F%3D=65536");
  }
}
