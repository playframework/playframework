/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import controllers.ShortController;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static controllers.routes.ShortController;
import static org.assertj.core.api.Assertions.assertThat;

public class ShortParamTest extends AbstractRoutesTest {

  private static final List<Short> defaultList = List.of((short) 1, (short) 2, (short) 3);
  private static final List<Short> testList = List.of((short) 7, (short) -8, (short) 9);

  /** Tests route {@code /short-p} {@link ShortController#path}. */
  @Test
  public void checkPath() {
    var path = "/short-p";
    // Correct value
    checkResult(path + "/789", okContains("789"));
    checkResult(path + "/-789", okContains("-789"));
    // Without param
    checkResult(path, this::notFound);
    // Incorrect value
    checkResult(path + "/40000", this::badRequestCannotParseParameter);
    checkResult(path + "/1.0", this::badRequestCannotParseParameter);
    checkResult(path + "/invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(ShortController.path(null).url()).isEqualTo(path + "/0");
    assertThat(ShortController.path((short) 789).url()).isEqualTo(path + "/789");
    assertThat(ShortController.path((short) -789).url()).isEqualTo(path + "/-789");
  }

  /** Tests route {@code /short} {@link ShortController#query}. */
  @Test
  public void checkQuery() {
    var path = "/short";
    // Correct value
    checkResult(path, "x=789", okContains("789"));
    checkResult(path, "x=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, this::badRequestMissingParameter);
    checkResult(path, "x", this::badRequestMissingParameter);
    checkResult(path, "x=", this::badRequestMissingParameter);
    // Incorrect value
    checkResult(path, "x=40000", this::badRequestCannotParseParameter);
    checkResult(path, "x=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(ShortController.query(null).url()).isEqualTo(path + "?x=0");
    assertThat(ShortController.query((short) 789).url()).isEqualTo(path + "?x=789");
    assertThat(ShortController.query((short) -789).url()).isEqualTo(path + "?x=-789");
  }

  /** Tests route {@code /short-d} {@link ShortController#queryDefault}. */
  @Test
  public void checkQueryDefault() {
    var path = "/short-d";
    // Correct value
    checkResult(path, "x?%3D=789", okContains("789"));
    checkResult(path, "x?%3D=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("123"));
    checkResult(path, "x?%3D", okContains("123"));
    checkResult(path, "x?%3D=", okContains("123"));
    // Incorrect value
    checkResult(path, "x?%3D=40000", this::badRequestCannotParseParameter);
    checkResult(path, "x?%3D=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(ShortController.queryDefault(null).url()).isEqualTo(path + "?x%3F%3D=0");
    assertThat(ShortController.queryDefault((short) 123).url()).isEqualTo(path);
    assertThat(ShortController.queryDefault((short) 789).url()).isEqualTo(path + "?x%3F%3D=789");
    assertThat(ShortController.queryDefault((short) -789).url()).isEqualTo(path + "?x%3F%3D=-789");
  }

  /** Tests route {@code /short-f} {@link ShortController#queryFixed}. */
  @Test
  public void checkQueryFixed() {
    var path = "/short-f";
    // Correct value
    checkResult(path, "x=789", okContains("123"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("123"));
    checkResult(path, "x", okContains("123"));
    checkResult(path, "x=", okContains("123"));
    // Incorrect value
    checkResult(path, "x=invalid", okContains("123"));
    // Reverse route
    assertThat(ShortController.queryFixed().url()).isEqualTo(path);
  }

  /** Tests route {@code /short-null} {@link ShortController#queryNullable}. */
  @Test
  public void checkQueryNullable() {
    var path = "/short-null";
    // Correct value
    checkResult(path, "x?=789", okContains("789"));
    checkResult(path, "x?=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x?", okContains("null"));
    checkResult(path, "x?=", okContains("null"));
    // Incorrect value
    checkResult(path, "x?=40000", this::badRequestCannotParseParameter);
    checkResult(path, "x?=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(ShortController.queryNullable(null).url()).isEqualTo(path);
    assertThat(ShortController.queryNullable((short) 789).url()).isEqualTo(path + "?x%3F=789");
    assertThat(ShortController.queryNullable((short) -789).url()).isEqualTo(path + "?x%3F=-789");
  }

  /** Tests route {@code /short-opt} {@link ShortController#queryOptional}. */
  @Test
  public void checkQueryOptional() {
    var path = "/short-opt";
    // Correct value
    checkResult(path, "x?=789", okContains("789"));
    checkResult(path, "x?=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmptyOptional);
    checkResult(path, "x?", this::okEmptyOptional);
    checkResult(path, "x?=", this::okEmptyOptional);
    // Incorrect value
    checkResult(path, "x?=40000", this::badRequestCannotParseParameter);
    checkResult(path, "x?=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(ShortController.queryOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(ShortController.queryOptional(Optional.of((short) 789)).url())
        .isEqualTo(path + "?x%3F=789");
    assertThat(ShortController.queryOptional(Optional.of((short) -789)).url())
        .isEqualTo(path + "?x%3F=-789");
  }

  /** Tests route {@code /short-opt-d} {@link ShortController#queryOptionalDefault}. */
  @Test
  public void checkQueryOptionalDefault() {
    var path = "/short-opt-d";
    // Correct value
    checkResult(path, "x?%3D=789", okContains("789"));
    checkResult(path, "x?%3D=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("123"));
    checkResult(path, "x?%3D", okContains("123"));
    checkResult(path, "x?%3D=", okContains("123"));
    // Incorrect value
    checkResult(path, "x?%3D=40000", this::badRequestCannotParseParameter);
    checkResult(path, "x?%3D=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(ShortController.queryOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(ShortController.queryOptionalDefault(Optional.of((short) 123)).url())
        .isEqualTo(path);
    assertThat(ShortController.queryOptionalDefault(Optional.of((short) 789)).url())
        .isEqualTo(path + "?x%3F%3D=789");
    assertThat(ShortController.queryOptionalDefault(Optional.of((short) -789)).url())
        .isEqualTo(path + "?x%3F%3D=-789");
  }

  /** Tests route {@code /short-list} {@link ShortController#queryList}. */
  @Test
  public void checkQueryList() {
    var path = "/short-list";
    // Correct value
    checkResult(path, "x[]=7&x[]=-8&x[]=", okContains("7,-8"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]", this::okEmpty);
    checkResult(path, "x[]=", this::okEmpty);
    // Incorrect value
    checkResult(path, "x[]=invalid", this::badRequestCannotParseParameter);
    checkResult(path, "x[]=40000", this::badRequestCannotParseParameter);
    checkResult(path, "x[]=1.0", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(ShortController.queryList(List.of()).url()).isEqualTo(path);
    assertThat(ShortController.queryList(testList).url())
        .isEqualTo(path + "?x%5B%5D=7&x%5B%5D=-8&x%5B%5D=9");
  }

  /** Tests route {@code /short-list-d} {@link ShortController#queryListDefault}. */
  @Test
  public void checkQueryListDefault() {
    var path = "/short-list-d";
    // Correct value
    checkResult(path, "x[]%3D=7&x[]%3D=-8&x[]%3D=", okContains("7,-8"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("1,2,3"));
    checkResult(path, "x[]%3D", okContains("1,2,3"));
    checkResult(path, "x[]%3D=", okContains("1,2,3"));
    // Incorrect value
    checkResult(path, "x[]%3D=1&x[]%3D=40000", this::badRequestCannotParseParameter);
    checkResult(path, "x[]%3D=1&x[]%3D=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x[]%3D=1&x[]%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(ShortController.queryListDefault(List.of()).url()).isEqualTo(path);
    assertThat(ShortController.queryListDefault(defaultList).url()).isEqualTo(path);
    assertThat(ShortController.queryListDefault(testList).url())
        .isEqualTo(path + "?x%5B%5D%3D=7&x%5B%5D%3D=-8&x%5B%5D%3D=9");
  }

  /** Tests route {@code /short-list-null} {@link ShortController#queryListNullable}. */
  @Test
  public void checkQueryListNullable() {
    var path = "/short-list-null";
    // Correct value
    checkResult(path, "x[]?=7&x[]?=-8&x[]?=", okContains("7,-8"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x[]?", okContains("null"));
    checkResult(path, "x[]?=", okContains("null"));
    // Incorrect value
    checkResult(path, "x[]?=1&x[]?=40000", this::badRequestCannotParseParameter);
    checkResult(path, "x[]?=1&x[]?=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x[]?=1&x[]?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(ShortController.queryListNullable(null).url()).isEqualTo(path);
    assertThat(ShortController.queryListNullable(List.of()).url()).isEqualTo(path);
    assertThat(ShortController.queryListNullable(testList).url())
        .isEqualTo(path + "?x%5B%5D%3F=7&x%5B%5D%3F=-8&x%5B%5D%3F=9");
  }

  /** Tests route {@code /short-list-opt} {@link ShortController#queryListOptional}. */
  @Test
  public void checkQueryListOptional() {
    var path = "/short-list-opt";
    // Correct value
    checkResult(path, "x[]?=7&x[]?=-8&x[]?=", okContains("7,-8"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]?", this::okEmpty);
    checkResult(path, "x[]?=", this::okEmpty);
    // Incorrect value
    checkResult(path, "x[]?=1&x[]?=40000", this::badRequestCannotParseParameter);
    checkResult(path, "x[]?=1&x[]?=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x[]?=1&x[]?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(ShortController.queryListOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(ShortController.queryListOptional(Optional.of(List.of())).url()).isEqualTo(path);
    assertThat(ShortController.queryListOptional(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F=7&x%5B%5D%3F=-8&x%5B%5D%3F=9");
  }

  /** Tests route {@code /short-list-opt-d} {@link ShortController#queryListOptionalDefault}. */
  @Test
  public void checkQueryListOptionalDefault() {
    var path = "/short-list-opt-d";
    // Correct value
    checkResult(path, "x[]?%3D=7&x[]?%3D=-8&x[]?%3D=", okContains("7,-8"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("1,2,3"));
    checkResult(path, "x[]?%3D", okContains("1,2,3"));
    checkResult(path, "x[]?%3D=", okContains("1,2,3"));
    // Incorrect value
    checkResult(path, "x[]?%3D=1&x[]?%3D=40000", this::badRequestCannotParseParameter);
    checkResult(path, "x[]?%3D=1&x[]?%3D=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x[]?%3D=1&x[]?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(ShortController.queryListOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(ShortController.queryListOptionalDefault(Optional.of(List.of())).url())
        .isEqualTo(path);
    assertThat(ShortController.queryListOptionalDefault(Optional.of(defaultList)).url())
        .isEqualTo(path);
    assertThat(ShortController.queryListOptionalDefault(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F%3D=7&x%5B%5D%3F%3D=-8&x%5B%5D%3F%3D=9");
  }
}
