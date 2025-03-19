/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static controllers.routes.BooleanController;
import static org.assertj.core.api.Assertions.assertThat;

import controllers.BooleanController;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class BooleanParamTest extends AbstractRoutesTest {

  /** Tests route {@code /bool-p} {@link BooleanController#path}. */
  @Test
  public void checkPath() {
    var path = "/bool-p";
    // Correct value
    checkResult(path + "/true", okContains("true"));
    checkResult(path + "/false", okContains("false"));
    checkResult(path + "/1", okContains("true"));
    checkResult(path + "/0", okContains("false"));
    // Without param
    checkResult(path, this::notFound);
    // Incorrect value
    checkResult(path + "/invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(BooleanController.path(null).url()).isEqualTo(path + "/false");
    assertThat(BooleanController.path(true).url()).isEqualTo(path + "/true");
    assertThat(BooleanController.path(false).url()).isEqualTo(path + "/false");
  }

  /** Tests route {@code /bool} {@link BooleanController#query}. */
  @Test
  public void checkQuery() {
    var path = "/bool";
    // Correct value
    checkResult(path, "x=true", okContains("true"));
    checkResult(path, "x=false", okContains("false"));
    checkResult(path, "x=1", okContains("true"));
    checkResult(path, "x=0", okContains("false"));
    // Without/Empty/NoValue param
    checkResult(path, this::badRequestMissingParameter);
    checkResult(path, "x", this::badRequestMissingParameter);
    checkResult(path, "x=", this::badRequestMissingParameter);
    // Incorrect value
    checkResult(path, "x=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(BooleanController.query(null).url()).isEqualTo(path + "?x=false");
    assertThat(BooleanController.query(true).url()).isEqualTo(path + "?x=true");
    assertThat(BooleanController.query(false).url()).isEqualTo(path + "?x=false");
  }

  /** Tests route {@code /bool-d} {@link controllers.BooleanController#queryDefault}. */
  @Test
  public void checkQueryDefault() {
    var path = "/bool-d";
    // Correct value
    checkResult(path, "x?%3D=false", okContains("false"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("true"));
    checkResult(path, "x?%3D=", okContains("true"));
    checkResult(path, "x?%3D=", okContains("true"));
    // Incorrect value
    checkResult(path, "x?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(BooleanController.queryDefault(null).url()).isEqualTo(path + "?x%3F%3D=false");
    assertThat(BooleanController.queryDefault(true).url()).isEqualTo(path);
    assertThat(BooleanController.queryDefault(false).url()).isEqualTo(path + "?x%3F%3D=false");
  }

  /** Tests route {@code /bool-f} {@link controllers.BooleanController#queryFixed}. */
  @Test
  public void checkQueryFixed() {
    var path = "/bool-f";
    // Correct value
    checkResult(path, "x=true", okContains("true"));
    checkResult(path, "x=false", okContains("true"));
    checkResult(path, "x=1", okContains("true"));
    checkResult(path, "x=0", okContains("true"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("true"));
    checkResult(path, "x", okContains("true"));
    checkResult(path, "x=", okContains("true"));
    // Incorrect value
    checkResult(path, "x=invalid", okContains("true"));
    // Reverse route
    assertThat(BooleanController.queryFixed().url()).isEqualTo(path);
  }

  /** Tests route {@code /bool-null} {@link controllers.BooleanController#queryNullable}. */
  @Test
  public void checkQueryNullable() {
    var path = "/bool-null";
    // Correct value
    checkResult(path, "x?=true", okContains("true"));
    checkResult(path, "x?=false", okContains("false"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x?", okContains("null"));
    checkResult(path, "x?=", okContains("null"));
    // Incorrect value
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(BooleanController.queryNullable(null).url()).isEqualTo(path);
    assertThat(BooleanController.queryNullable(true).url()).isEqualTo(path + "?x%3F=true");
    assertThat(BooleanController.queryNullable(false).url()).isEqualTo(path + "?x%3F=false");
  }

  /** Tests route {@code /bool-opt} {@link controllers.BooleanController#queryOptional}. */
  @Test
  public void checkQueryOptional() {
    var path = "/bool-opt";
    // Correct value
    checkResult(path, "x?=true", okContains("true"));
    checkResult(path, "x?=false", okContains("false"));
    checkResult(path, "x?=1", okContains("true"));
    checkResult(path, "x?=0", okContains("false"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmptyOptional);
    checkResult(path, "x?", this::okEmptyOptional);
    checkResult(path, "x?=", this::okEmptyOptional);
    // Incorrect value
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(BooleanController.queryOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(BooleanController.queryOptional(Optional.of(true)).url())
        .isEqualTo(path + "?x%3F=true");
    assertThat(BooleanController.queryOptional(Optional.of(false)).url())
        .isEqualTo(path + "?x%3F=false");
  }

  /** Tests route {@code /bool-opt-d} {@link controllers.BooleanController#queryOptionalDefault}. */
  @Test
  public void checkQueryOptionalDefault() {
    var path = "/bool-opt-d";
    // Correct value
    checkResult(path, "x?%3D=true", okContains("true"));
    checkResult(path, "x?%3D=false", okContains("false"));
    checkResult(path, "x?%3D=1", okContains("true"));
    checkResult(path, "x?%3D=0", okContains("false"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("true"));
    checkResult(path, "x?%3D", okContains("true"));
    checkResult(path, "x?%3D=", okContains("true"));
    // Incorrect value
    checkResult(path, "x?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(BooleanController.queryOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(BooleanController.queryOptionalDefault(Optional.of(true)).url()).isEqualTo(path);
    assertThat(BooleanController.queryOptionalDefault(Optional.of(false)).url())
        .isEqualTo(path + "?x%3F%3D=false");
  }

  /** Tests route {@code /bool-list} {@link controllers.BooleanController#queryList}. */
  @Test
  public void checkQueryList() {
    var path = "/bool-list";
    // Correct value
    checkResult(path, "x[]=true&x[]=false&x[]=true", okContains("true,false,true"));
    checkResult(path, "x[]=1&x[]=0&x[]=1", okContains("true,false,true"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]", this::okEmpty);
    checkResult(path, "x[]=", this::okEmpty);
    // Incorrect value
    checkResult(path, "x[]=1&x[]=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(BooleanController.queryList(List.of()).url()).isEqualTo(path);
    assertThat(BooleanController.queryList(List.of(true, false, true)).url())
        .isEqualTo(path + "?x%5B%5D=true&x%5B%5D=false&x%5B%5D=true");
  }

  /** Tests route {@code /bool-list-d} {@link controllers.BooleanController#queryListDefault}. */
  @Test
  public void checkQueryListDefault() {
    var path = "/bool-list-d";
    // Correct value
    checkResult(path, "x[]%3D=false&x[]%3D=false&x[]%3D=true", okContains("false,false,true"));
    checkResult(path, "x[]%3D=0&x[]%3D=0&x[]%3D=1", okContains("false,false,true"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("true,false,true"));
    checkResult(path, "x[]%3D", okContains("true,false,true"));
    checkResult(path, "x[]%3D=", okContains("true,false,true"));
    // Incorrect value
    checkResult(path, "x[]%3D=1&x[]%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(BooleanController.queryListDefault(List.of()).url()).isEqualTo(path);
    assertThat(BooleanController.queryListDefault(List.of(true, false, true)).url())
        .isEqualTo(path);
    assertThat(BooleanController.queryListDefault(List.of(false, false, true)).url())
        .isEqualTo(path + "?x%5B%5D%3D=false&x%5B%5D%3D=false&x%5B%5D%3D=true");
  }

  /**
   * Tests route {@code /bool-list-null} {@link controllers.BooleanController#queryListNullable}.
   */
  @Test
  public void checkQueryListNullable() {
    var path = "/bool-list-null";
    // Correct value
    checkResult(path, "x[]?=true&x[]?=false&x[]?=true", okContains("true,false,true"));
    checkResult(path, "x[]?=1&x[]?=0&x[]?=1", okContains("true,false,true"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x[]?", okContains("null"));
    checkResult(path, "x[]?=", okContains("null"));
    // Incorrect value
    checkResult(path, "x[]?=1&x[]?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(BooleanController.queryListNullable(null).url()).isEqualTo(path);
    assertThat(BooleanController.queryListNullable(List.of()).url()).isEqualTo(path);
    assertThat(BooleanController.queryListNullable(List.of(true, false, true)).url())
        .isEqualTo(path + "?x%5B%5D%3F=true&x%5B%5D%3F=false&x%5B%5D%3F=true");
  }

  /** Tests route {@code /bool-list-opt} {@link controllers.BooleanController#queryListOptional}. */
  @Test
  public void checkQueryListOptional() {
    var path = "/bool-list-opt";
    // Correct value
    checkResult(path, "x[]?=true&x[]?=false&x[]?=true", okContains("true,false,true"));
    checkResult(path, "x[]?=1&x[]?=0&x[]?=1", okContains("true,false,true"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]?", this::okEmpty);
    checkResult(path, "x[]?=", this::okEmpty);
    // Incorrect value
    checkResult(path, "x[]?=1&x[]?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(BooleanController.queryListOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(BooleanController.queryListOptional(Optional.of(List.of())).url()).isEqualTo(path);
    assertThat(BooleanController.queryListOptional(Optional.of(List.of(true, false, true))).url())
        .isEqualTo(path + "?x%5B%5D%3F=true&x%5B%5D%3F=false&x%5B%5D%3F=true");
  }

  /**
   * Tests route {@code /bool-list-opt-d} {@link
   * controllers.BooleanController#queryListOptionalDefault}.
   */
  @Test
  public void checkQueryListOptionalDefault() {
    var path = "/bool-list-opt-d";
    // Correct value
    checkResult(path, "x[]?%3D=false&x[]?%3D=false&x[]?%3D=true", okContains("false,false,true"));
    checkResult(path, "x[]?%3D=0&x[]?%3D=0&x[]?%3D=1", okContains("false,false,true"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("true,false,true"));
    checkResult(path, "x[]?%3D", okContains("true,false,true"));
    checkResult(path, "x[]?%3D=", okContains("true,false,true"));
    // Incorrect value
    checkResult(path, "x[]?%3D=1&x[]?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(BooleanController.queryListOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(BooleanController.queryListOptionalDefault(Optional.of(List.of())).url())
        .isEqualTo(path);
    assertThat(
            BooleanController.queryListOptionalDefault(Optional.of(List.of(true, false, true)))
                .url())
        .isEqualTo(path);
    assertThat(
            BooleanController.queryListOptionalDefault(Optional.of(List.of(false, false, true)))
                .url())
        .isEqualTo(path + "?x%5B%5D%3F%3D=false&x%5B%5D%3F%3D=false&x%5B%5D%3F%3D=true");
  }
}
