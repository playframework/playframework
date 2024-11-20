/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static controllers.routes.CharacterController;
import static org.assertj.core.api.Assertions.assertThat;

import controllers.CharacterController;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

@SuppressWarnings("NonAsciiCharacters")
public class CharacterParamTest extends AbstractRoutesTest {

  private static final List<Character> defaultList = List.of('a', 'b', 'c');
  private static final List<Character> testList = List.of('x', 'y', 'z');
  private static final List<Character> unicodeList = List.of('π', 'ε');

  /** Tests route {@code /char-p} {@link CharacterController#path}. */
  @Test
  public void checkPath() {
    var path = "/char-p";
    // Correct value
    checkResult(path + "/z", okContains("z"));
    checkResult(path + "/π", okContains("π"));
    checkResult(path + "/%CF%80", okContains("π"));
    // Without param
    checkResult(path, this::notFound);
    // Incorrect value
    checkResult(path + "/invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(CharacterController.path(null).url()).isEqualTo(path + "/%00");
    assertThat(CharacterController.path('z').url()).isEqualTo(path + "/z");
    assertThat(CharacterController.path('π').url()).isEqualTo(path + "/%CF%80");
  }

  /** Tests route {@code /char} {@link CharacterController#query}. */
  @Test
  public void checkQuery() {
    var path = "/char";
    // Correct value
    checkResult(path, "x=z", okContains("z"));
    checkResult(path, "x=π", okContains("π"));
    checkResult(path, "x=%CF%80", okContains("π"));
    // Without/Empty/NoValue param
    checkResult(path, this::badRequestMissingParameter);
    checkResult(path, "x", this::badRequestMissingParameter);
    checkResult(path, "x=", this::badRequestMissingParameter);
    // Incorrect value
    checkResult(path, "x=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(CharacterController.query(null).url()).isEqualTo(path + "?x=%00");
    assertThat(CharacterController.query('z').url()).isEqualTo(path + "?x=z");
    assertThat(CharacterController.query('π').url()).isEqualTo(path + "?x=%CF%80");
  }

  /** Tests route {@code /char-d} {@link controllers.CharacterController#queryDefault}. */
  @Test
  public void checkQueryDefault() {
    var path = "/char-d";
    // Correct value
    checkResult(path, "x?%3D=z", okContains("z"));
    checkResult(path, "x?%3D=π", okContains("π"));
    checkResult(path, "x?%3D=%CF%80", okContains("π"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("a"));
    checkResult(path, "x?%3D", okContains("a"));
    checkResult(path, "x?%3D=", okContains("a"));
    // Incorrect value
    checkResult(path, "x?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(CharacterController.queryDefault(null).url()).isEqualTo(path + "?x%3F%3D=%00");
    assertThat(CharacterController.queryDefault('a').url()).isEqualTo(path);
    assertThat(CharacterController.queryDefault('z').url()).isEqualTo(path + "?x%3F%3D=z");
    assertThat(CharacterController.queryDefault('π').url()).isEqualTo(path + "?x%3F%3D=%CF%80");
  }

  /** Tests route {@code /char-f} {@link controllers.CharacterController#queryFixed}. */
  @Test
  public void checkQueryFixed() {
    var path = "/char-f";
    // Correct value
    checkResult(path, "x=z", okContains("a"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("a"));
    checkResult(path, "x", okContains("a"));
    checkResult(path, "x=", okContains("a"));
    // Incorrect value
    checkResult(path, "x=invalid", okContains("a"));
    // Reverse route
    assertThat(CharacterController.queryFixed().url()).isEqualTo(path);
  }

  /** Tests route {@code /char-null} {@link controllers.CharacterController#queryNullable}. */
  @Test
  public void checkQueryNullable() {
    var path = "/char-null";
    // Correct value
    checkResult(path, "x?=z", okContains("z"));
    checkResult(path, "x?=π", okContains("π"));
    checkResult(path, "x?=%CF%80", okContains("π"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x?", okContains("null"));
    checkResult(path, "x?=", okContains("null"));
    // Incorrect value
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(CharacterController.queryNullable(null).url()).isEqualTo(path);
    assertThat(CharacterController.queryNullable('z').url()).isEqualTo(path + "?x%3F=z");
    assertThat(CharacterController.queryNullable('π').url()).isEqualTo(path + "?x%3F=%CF%80");
  }

  /** Tests route {@code /char-opt} {@link controllers.CharacterController#queryOptional}. */
  @Test
  public void checkQueryOptional() {
    var path = "/char-opt";
    // Correct value
    checkResult(path, "x?=z", okContains("z"));
    checkResult(path, "x?=π", okContains("π"));
    checkResult(path, "x?=%CF%80", okContains("π"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmptyOptional);
    checkResult(path, "x?", this::okEmptyOptional);
    checkResult(path, "x?=", this::okEmptyOptional);
    // Incorrect value
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(CharacterController.queryOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(CharacterController.queryOptional(Optional.of('z')).url())
        .isEqualTo(path + "?x%3F=z");
    assertThat(CharacterController.queryOptional(Optional.of('π')).url())
        .isEqualTo(path + "?x%3F=%CF%80");
  }

  /**
   * Tests route {@code /char-opt-d} {@link controllers.CharacterController#queryOptionalDefault}.
   */
  @Test
  public void checkQueryOptionalDefault() {
    var path = "/char-opt-d";
    // Correct value
    checkResult(path, "x?%3D=z", okContains("z"));
    checkResult(path, "x?%3D=π", okContains("π"));
    checkResult(path, "x?%3D=%CF%80", okContains("π"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("a"));
    checkResult(path, "x?%3D", okContains("a"));
    checkResult(path, "x?%3D=", okContains("a"));
    // Incorrect value
    checkResult(path, "x?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(CharacterController.queryOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(CharacterController.queryOptionalDefault(Optional.of('a')).url()).isEqualTo(path);
    assertThat(CharacterController.queryOptionalDefault(Optional.of('z')).url())
        .isEqualTo(path + "?x%3F%3D=z");
    assertThat(CharacterController.queryOptionalDefault(Optional.of('π')).url())
        .isEqualTo(path + "?x%3F%3D=%CF%80");
  }

  /** Tests route {@code /char-list} {@link controllers.CharacterController#queryList}. */
  @Test
  public void checkQueryList() {
    var path = "/char-list";
    // Correct value
    checkResult(path, "x[]=a&x[]=b&x[]=c", okContains("a,b,c"));
    checkResult(path, "x[]=π&x[]=ε&x[]=%CF%80&", okContains("π,ε,π"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]", this::okEmpty);
    checkResult(path, "x[]=", this::okEmpty);
    // Incorrect value
    checkResult(path, "x[]=z&x[]=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(CharacterController.queryList(List.of()).url()).isEqualTo(path);
    assertThat(CharacterController.queryList(defaultList).url())
        .isEqualTo(path + "?x%5B%5D=a&x%5B%5D=b&x%5B%5D=c");
    assertThat(CharacterController.queryList(unicodeList).url())
        .isEqualTo(path + "?x%5B%5D=%CF%80&x%5B%5D=%CE%B5");
  }

  /** Tests route {@code /char-list-d} {@link controllers.CharacterController#queryListDefault}. */
  @Test
  public void checkQueryListDefault() {
    var path = "/char-list-d";
    // Correct value
    checkResult(path, "x[]%3D=x&x[]%3D=y&x[]%3D=z", okContains("x,y,z"));
    checkResult(path, "x[]%3D=π&x[]%3D=ε&x[]%3D=%CF%80&", okContains("π,ε,π"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("a,b,c"));
    checkResult(path, "x[]%3D", okContains("a,b,c"));
    checkResult(path, "x[]%3D=", okContains("a,b,c"));
    // Incorrect value
    checkResult(path, "x[]%3D=1&x[]%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(CharacterController.queryListDefault(List.of()).url()).isEqualTo(path);
    assertThat(CharacterController.queryListDefault(defaultList).url()).isEqualTo(path);
    assertThat(CharacterController.queryListDefault(testList).url())
        .isEqualTo(path + "?x%5B%5D%3D=x&x%5B%5D%3D=y&x%5B%5D%3D=z");
    assertThat(CharacterController.queryListDefault(unicodeList).url())
        .isEqualTo(path + "?x%5B%5D%3D=%CF%80&x%5B%5D%3D=%CE%B5");
  }

  /**
   * Tests route {@code /char-list-null} {@link controllers.CharacterController#queryListNullable}.
   */
  @Test
  public void checkQueryListNullable() {
    var path = "/char-list-null";
    // Correct value
    checkResult(path, "x[]?=x&x[]?=y&x[]?=z", okContains("x,y,z"));
    checkResult(path, "x[]?=π&x[]?=ε&x[]?=%CF%80&", okContains("π,ε,π"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x[]?", okContains("null"));
    checkResult(path, "x[]?=", okContains("null"));
    // Incorrect value
    checkResult(path, "x[]?=1&x[]?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(CharacterController.queryListNullable(null).url()).isEqualTo(path);
    assertThat(CharacterController.queryListNullable(List.of()).url()).isEqualTo(path);
    assertThat(CharacterController.queryListNullable(testList).url())
        .isEqualTo(path + "?x%5B%5D%3F=x&x%5B%5D%3F=y&x%5B%5D%3F=z");
    assertThat(CharacterController.queryListNullable(unicodeList).url())
        .isEqualTo(path + "?x%5B%5D%3F=%CF%80&x%5B%5D%3F=%CE%B5");
  }

  /**
   * Tests route {@code /char-list-opt} {@link controllers.CharacterController#queryListOptional}.
   */
  @Test
  public void checkQueryListOptional() {
    var path = "/char-list-opt";
    // Correct value
    checkResult(path, "x[]?=x&x[]?=y&x[]?=z", okContains("x,y,z"));
    checkResult(path, "x[]?=π&x[]?=ε&x[]?=%CF%80&", okContains("π,ε,π"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]?", this::okEmpty);
    checkResult(path, "x[]?=", this::okEmpty);
    // Incorrect value
    checkResult(path, "x[]?=1&x[]?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(CharacterController.queryListOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(CharacterController.queryListOptional(Optional.of(List.of())).url()).isEqualTo(path);
    assertThat(CharacterController.queryListOptional(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F=x&x%5B%5D%3F=y&x%5B%5D%3F=z");
    assertThat(CharacterController.queryListOptional(Optional.of(unicodeList)).url())
        .isEqualTo(path + "?x%5B%5D%3F=%CF%80&x%5B%5D%3F=%CE%B5");
  }

  /**
   * Tests route {@code /char-list-opt-d} {@link
   * controllers.CharacterController#queryListOptionalDefault}.
   */
  @Test
  public void checkQueryListOptionalDefault() {
    var path = "/char-list-opt-d";
    // Correct value
    checkResult(path, "x[]?%3D=x&x[]?%3D=y&x[]?%3D=z", okContains("x,y,z"));
    checkResult(path, "x[]?%3D=π&x[]?%3D=ε&x[]?%3D=%CF%80&", okContains("π,ε,π"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("a,b,c"));
    checkResult(path, "x[]?%3D", okContains("a,b,c"));
    checkResult(path, "x[]?%3D=", okContains("a,b,c"));
    // Incorrect value
    checkResult(path, "x[]?%3D=1&x[]?%3D=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThat(CharacterController.queryListOptionalDefault(Optional.empty()).url())
        .isEqualTo(path);
    assertThat(CharacterController.queryListOptionalDefault(Optional.of(List.of())).url())
        .isEqualTo(path);
    assertThat(CharacterController.queryListOptionalDefault(Optional.of(defaultList)).url())
        .isEqualTo(path);
    assertThat(CharacterController.queryListOptionalDefault(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F%3D=x&x%5B%5D%3F%3D=y&x%5B%5D%3F%3D=z");
    assertThat(CharacterController.queryListOptionalDefault(Optional.of(unicodeList)).url())
        .isEqualTo(path + "?x%5B%5D%3F%3D=%CF%80&x%5B%5D%3F%3D=%CE%B5");
  }
}
