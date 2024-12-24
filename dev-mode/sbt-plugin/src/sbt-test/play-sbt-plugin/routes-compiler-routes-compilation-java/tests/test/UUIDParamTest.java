/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static controllers.routes.UUIDController;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import controllers.UUIDController;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;

public class UUIDParamTest extends AbstractRoutesTest {

  static final UUID defaultUUID = UUID.fromString("a90e582b-3c5b-4d37-b5f4-7730d2c672dd");
  static final UUID testUUID = UUID.fromString("2ef841cd-0fe0-423c-83ed-71040a1f42fe");
  private static final List<UUID> defaultList =
      List.of(defaultUUID, UUID.fromString("fff71165-0ae2-47d0-9151-3afe742c6351"));
  private static final List<UUID> testList =
      List.of(testUUID, UUID.fromString("5dee2e50-32c3-4ffc-a57c-18014e5eaf82"));

  /** Tests route {@code /uuid-p} {@link UUIDController#path}. */
  @Test
  public void checkPath() {
    var path = "/uuid-p";
    // Correct value
    checkResult(path + "/" + testUUID, okContains(testUUID.toString()));
    // Without param
    checkResult(path, this::notFound);
    // Incorrect value
    checkResult(path + "/invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThatThrownBy(() -> UUIDController.path(null).url())
        .isInstanceOf(NullPointerException.class);
    assertThat(UUIDController.path(testUUID).url()).isEqualTo(path + "/" + testUUID);
  }

  /** Tests route {@code /uuid} {@link UUIDController#query}. */
  @Test
  public void checkQuery() {
    var path = "/uuid";
    // Correct value
    checkResult(path, "x=" + testUUID, okContains(testUUID.toString()));
    // Without/Empty/NoValue param
    checkResult(path, this::badRequestMissingParameter);
    checkResult(path, "x", this::badRequestMissingParameter);
    checkResult(path, "x=", this::badRequestMissingParameter);
    // Incorrect value
    checkResult(path, "x=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThatThrownBy(() -> UUIDController.query(null).url())
        .isInstanceOf(NullPointerException.class);
    assertThat(UUIDController.query(testUUID).url()).isEqualTo(path + "?x=" + testUUID);
  }

  /** Tests route {@code /uuid-d} {@link UUIDController#queryDefault}. */
  @Test
  public void checkQueryDefault() {
    var path = "/uuid-d";
    // Correct value
    checkResult(path, "x?%3D=" + testUUID, okContains(testUUID.toString()));
    // Without/Empty/NoValue param
    checkResult(path, okContains(defaultUUID.toString()));
    checkResult(path, "x?%3D", okContains(defaultUUID.toString()));
    checkResult(path, "x?%3D=", okContains(defaultUUID.toString()));
    // Incorrect value
    checkResult(path, "x?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThatThrownBy(() -> UUIDController.queryDefault(null).url())
        .isInstanceOf(NullPointerException.class);
    assertThat(UUIDController.queryDefault(defaultUUID).url()).isEqualTo(path);
    assertThat(UUIDController.queryDefault(testUUID).url())
        .isEqualTo(path + "?x%3F%3D=" + testUUID);
  }

  /** Tests route {@code /uuid-f} {@link UUIDController#queryFixed}. */
  @Test
  public void checkQueryFixed() {
    var path = "/uuid-f";
    // Correct value
    checkResult(path, "x" + testUUID, okContains(defaultUUID.toString()));
    // Without/Empty/NoValue param
    checkResult(path, okContains(defaultUUID.toString()));
    checkResult(path, "x", okContains(defaultUUID.toString()));
    checkResult(path, "x=", okContains(defaultUUID.toString()));
    // Incorrect value
    checkResult(path, "x=invalid", okContains(defaultUUID.toString()));
    // Reverse route
    assertThat(UUIDController.queryFixed().url()).isEqualTo(path);
  }

  /** Tests route {@code /uuid-null} {@link UUIDController#queryNullable}. */
  @Test
  public void checkQueryNullable() {
    var path = "/uuid-null";
    // Correct value
    checkResult(path, "x?=" + testUUID, okContains(testUUID.toString()));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x?", okContains("null"));
    checkResult(path, "x?=", okContains("null"));
    // Incorrect value
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(UUIDController.queryNullable(null).url()).isEqualTo(path);
    assertThat(UUIDController.queryNullable(testUUID).url()).isEqualTo(path + "?x%3F=" + testUUID);
  }

  /** Tests route {@code /uuid-opt} {@link UUIDController#queryOptional}. */
  @Test
  public void checkQueryOptional() {
    var path = "/uuid-opt";
    // Correct value
    checkResult(path, "x?=" + testUUID, okContains(testUUID.toString()));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmptyOptional);
    checkResult(path, "x?", this::okEmptyOptional);
    checkResult(path, "x?=", this::okEmptyOptional);
    // Incorrect value
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(UUIDController.queryOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(UUIDController.queryOptional(Optional.of(testUUID)).url())
        .isEqualTo(path + "?x%3F=" + testUUID);
  }

  /** Tests route {@code /uuid-opt-d} {@link UUIDController#queryOptionalDefault}. */
  @Test
  public void checkQueryOptionalDefault() {
    var path = "/uuid-opt-d";
    // Correct value
    checkResult(path, "x?%3D=" + testUUID, okContains(testUUID.toString()));
    // Without/Empty/NoValue param
    checkResult(path, okContains(defaultUUID.toString()));
    checkResult(path, "x?%3D", okContains(defaultUUID.toString()));
    checkResult(path, "x?%3D=", okContains(defaultUUID.toString()));
    // Incorrect value
    checkResult(path, "x?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(UUIDController.queryOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(UUIDController.queryOptionalDefault(Optional.of(defaultUUID)).url()).isEqualTo(path);
    assertThat(UUIDController.queryOptionalDefault(Optional.of(testUUID)).url())
        .isEqualTo(path + "?x%3F%3D=" + testUUID);
  }

  /** Tests route {@code /uuid-list} {@link UUIDController#queryList}. */
  @Test
  public void checkQueryList() {
    var path = "/uuid-list";
    // Correct value
    checkResult(
        path,
        String.format("x[]=%s&x[]=%s&x[]=", testList.get(0), testList.get(1)),
        okContains(testList.get(0) + "," + testList.get(1)));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]", this::okEmpty);
    checkResult(path, "x[]=", this::okEmpty);
    // Incorrect value
    checkResult(path, "x[]=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(UUIDController.queryList(List.of()).url()).isEqualTo(path);
    assertThat(UUIDController.queryList(testList).url())
        .isEqualTo(path + "?x%5B%5D=" + testList.get(0) + "&x%5B%5D=" + testList.get(1));
  }

  /** Tests route {@code /uuid-list-d} {@link UUIDController#queryListDefault}. */
  @Test
  public void checkQueryListDefault() {
    var path = "/uuid-list-d";
    // Correct value
    checkResult(
        path,
        String.format("x[]%%3D=%s&x[]%%3D=%s&x[]%%3D=", testList.get(0), testList.get(1)),
        okContains(testList.get(0) + "," + testList.get(1)));
    // Without/Empty/NoValue param
    checkResult(path, okContains(defaultList.get(0) + "," + defaultList.get(1)));
    checkResult(path, "x[]%3D", okContains(defaultList.get(0) + "," + defaultList.get(1)));
    checkResult(path, "x[]%3D=", okContains(defaultList.get(0) + "," + defaultList.get(1)));
    // Incorrect value
    checkResult(path, "x[]%3D=1&x[]%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(UUIDController.queryListDefault(List.of()).url()).isEqualTo(path);
    assertThat(UUIDController.queryListDefault(defaultList).url()).isEqualTo(path);
    assertThat(UUIDController.queryListDefault(testList).url())
        .isEqualTo(path + "?x%5B%5D%3D=" + testList.get(0) + "&x%5B%5D%3D=" + testList.get(1));
  }

  /** Tests route {@code /uuid-list-null} {@link UUIDController#queryListNullable}. */
  @Test
  public void checkQueryListNullable() {
    var path = "/uuid-list-null";
    // Correct value
    checkResult(
        path,
        String.format("x[]?=%s&x[]?=%s&x[]?=", testList.get(0), testList.get(1)),
        okContains(testList.get(0) + "," + testList.get(1)));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x[]?", okContains("null"));
    checkResult(path, "x[]?=", okContains("null"));
    // Incorrect value
    checkResult(path, "x[]?=1&x[]?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(UUIDController.queryListNullable(null).url()).isEqualTo(path);
    assertThat(UUIDController.queryListNullable(List.of()).url()).isEqualTo(path);
    assertThat(UUIDController.queryListNullable(testList).url())
        .isEqualTo(path + "?x%5B%5D%3F=" + testList.get(0) + "&x%5B%5D%3F=" + testList.get(1));
  }

  /** Tests route {@code /uuid-list-opt} {@link UUIDController#queryListOptional}. */
  @Test
  public void checkQueryListOptional() {
    var path = "/uuid-list-opt";
    // Correct value
    checkResult(
        path,
        String.format("x[]?=%s&x[]?=%s&x[]?=", testList.get(0), testList.get(1)),
        okContains(testList.get(0) + "," + testList.get(1)));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]?", this::okEmpty);
    checkResult(path, "x[]?=", this::okEmpty);
    // Incorrect value
    checkResult(path, "x[]?=1&x[]?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(UUIDController.queryListOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(UUIDController.queryListOptional(Optional.of(List.of())).url()).isEqualTo(path);
    assertThat(UUIDController.queryListOptional(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F=" + testList.get(0) + "&x%5B%5D%3F=" + testList.get(1));
  }

  /** Tests route {@code /uuid-list-opt-d} {@link UUIDController#queryListOptionalDefault}. */
  @Test
  public void checkQueryListOptionalDefault() {
    var path = "/uuid-list-opt-d";
    // Correct value
    checkResult(
        path,
        String.format("x[]?%%3D=%s&x[]?%%3D=%s&x[]?%%3D=", testList.get(0), testList.get(1)),
        okContains(testList.get(0) + "," + testList.get(1)));
    // Without/Empty/NoValue param
    checkResult(path, okContains(defaultList.get(0) + "," + defaultList.get(1)));
    checkResult(path, "x[]?%3D", okContains(defaultList.get(0) + "," + defaultList.get(1)));
    checkResult(path, "x[]?%3D=", okContains(defaultList.get(0) + "," + defaultList.get(1)));
    // Incorrect value
    checkResult(path, "x[]?%3D=1&x[]?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(UUIDController.queryListOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(UUIDController.queryListOptionalDefault(Optional.of(List.of())).url())
        .isEqualTo(path);
    assertThat(UUIDController.queryListOptionalDefault(Optional.of(defaultList)).url())
        .isEqualTo(path);
    assertThat(UUIDController.queryListOptionalDefault(Optional.of(testList)).url())
        .isEqualTo(
            path + "?x%5B%5D%3F%3D=" + testList.get(0) + "&x%5B%5D%3F%3D=" + testList.get(1));
  }
}
