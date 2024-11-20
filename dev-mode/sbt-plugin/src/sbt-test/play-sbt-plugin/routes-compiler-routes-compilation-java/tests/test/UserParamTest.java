/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static controllers.routes.UserController;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import controllers.UserController;
import java.util.List;
import java.util.Optional;
import models.UserPathBindable;
import models.UserQueryBindable;
import org.junit.Test;

public class UserParamTest extends AbstractRoutesTest {

  private static final List<UserQueryBindable> defaultList =
      List.of(new UserQueryBindable("111"), new UserQueryBindable("222"));
  private static final List<UserQueryBindable> testList =
      List.of(new UserQueryBindable("333"), new UserQueryBindable("444"));

  /** Tests route {@code /user-p} {@link UserController#path(UserPathBindable)}. */
  @Test
  public void checkPath() {
    var path = "/user-p";
    // Correct value
    checkResult(path + "/789", okContains("User(789)"));
    // Without param
    checkResult(path, this::notFound);
    // Reverse route
    assertThatThrownBy(() -> UserController.path(null).url())
        .isInstanceOf(NullPointerException.class);
    assertThat(UserController.path(new UserPathBindable("789")).url()).isEqualTo(path + "/789");
    assertThat(UserController.path(new UserPathBindable("foo%bar")).url())
        .isEqualTo(path + "/foo%25bar");
  }

  /** Tests route {@code /user} {@link UserController#query}. */
  @Test
  public void checkQuery() {
    var path = "/user";
    // Correct value
    checkResult(path, "x=789", okContains("User(789)"));
    // Without/Empty/NoValue param
    checkResult(path, this::badRequestMissingParameter);
    checkResult(path, "x", this::badRequestMissingParameter);
    checkResult(path, "x=", this::badRequestMissingParameter);
    // Reverse route
    assertThatThrownBy(() -> UserController.query(null).url())
        .isInstanceOf(NullPointerException.class);
    assertThat(UserController.query(new UserQueryBindable("789")).url()).isEqualTo(path + "?x=789");
    assertThat(UserController.query(new UserQueryBindable("foo/bar")).url())
        .isEqualTo(path + "?x=foo%2Fbar");
  }

  /** Tests route {@code /user-d} {@link UserController#queryDefault}. */
  @Test
  public void checkQueryDefault() {
    var path = "/user-d";
    // Correct value
    checkResult(path, "x?%3D=789", okContains("User(789)"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("User(123)"));
    checkResult(path, "x?%3D", okContains("User(123)"));
    checkResult(path, "x?%3D=", okContains("User(123)"));
    // Reverse route
    assertThatThrownBy(() -> UserController.queryDefault(null).url())
        .isInstanceOf(NullPointerException.class);
    assertThat(UserController.queryDefault(new UserQueryBindable("123")).url()).isEqualTo(path);
    assertThat(UserController.queryDefault(new UserQueryBindable("789")).url())
        .isEqualTo(path + "?x%3F%3D=789");
  }

  /** Tests route {@code /user-f} {@link UserController#queryFixed}. */
  @Test
  public void checkQueryFixed() {
    var path = "/user-f";
    // Correct value
    checkResult(path, "x=789", okContains("User(123)"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("User(123)"));
    checkResult(path, "x", okContains("User(123)"));
    checkResult(path, "x=", okContains("User(123)"));
    // Incorrect value
    checkResult(path, "x=invalid", okContains("User(123)"));
    // Reverse route
    assertThat(UserController.queryFixed().url()).isEqualTo(path);
  }

  /** Tests route {@code /user-null} {@link UserController#queryNullable}. */
  @Test
  public void checkQueryNullable() {
    var path = "/user-null";
    // Correct value
    checkResult(path, "x?=789", okContains("User(789)"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x?", okContains("null"));
    checkResult(path, "x?=", okContains("null"));
    // Reverse route
    assertThat(UserController.queryNullable(null).url()).isEqualTo(path);
    assertThat(UserController.queryNullable(new UserQueryBindable("789")).url())
        .isEqualTo(path + "?x%3F=789");
  }

  /** Tests route {@code /user-opt} {@link UserController#queryOptional}. */
  @Test
  public void checkQueryOptional() {
    var path = "/user-opt";
    // Correct value
    checkResult(path, "x?=789", okContains("User(789)"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmptyOptional);
    checkResult(path, "x?", this::okEmptyOptional);
    checkResult(path, "x?=", this::okEmptyOptional);
    // Reverse route
    assertThat(UserController.queryOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(UserController.queryOptional(Optional.of(new UserQueryBindable("789"))).url())
        .isEqualTo(path + "?x%3F=789");
  }

  /** Tests route {@code /user-opt-d} {@link UserController#queryOptionalDefault}. */
  @Test
  public void checkQueryOptionalDefault() {
    var path = "/user-opt-d";
    // Correct value
    checkResult(path, "x?%3D=789", okContains("User(789)"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("User(123)"));
    checkResult(path, "x?%3D", okContains("User(123)"));
    checkResult(path, "x?%3D=", okContains("User(123)"));
    // Reverse route
    assertThat(UserController.queryOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(UserController.queryOptionalDefault(Optional.of(new UserQueryBindable("123"))).url())
        .isEqualTo(path);
    assertThat(UserController.queryOptionalDefault(Optional.of(new UserQueryBindable("789"))).url())
        .isEqualTo(path + "?x%3F%3D=789");
  }

  /** Tests route {@code /user-list} {@link UserController#queryList}. */
  @Test
  public void checkQueryList() {
    var path = "/user-list";
    // Correct value
    checkResult(path, "x[]=333&x[]=444", okContains("User(333),User(444)"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]", this::okEmpty);
    checkResult(path, "x[]=", this::okEmpty);
    // Reverse route
    assertThat(UserController.queryList(List.of()).url()).isEqualTo(path);
    assertThat(UserController.queryList(testList).url())
        .isEqualTo(path + "?x%5B%5D=333&x%5B%5D=444");
  }

  /** Tests route {@code /user-list-d} {@link UserController#queryListDefault}. */
  @Test
  public void checkQueryListDefault() {
    var path = "/user-list-d";
    // Correct value
    checkResult(path, "x[]%3D=333&x[]%3D=444", okContains("User(333),User(444)"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("User(111),User(222)"));
    checkResult(path, "x[]%3D", okContains("User(111),User(222)"));
    checkResult(path, "x[]%3D=", okContains("User(111),User(222)"));
    // Reverse route
    assertThat(UserController.queryListDefault(List.of()).url()).isEqualTo(path);
    assertThat(UserController.queryListDefault(defaultList).url()).isEqualTo(path);
    assertThat(UserController.queryListDefault(testList).url())
        .isEqualTo(path + "?x%5B%5D%3D=333&x%5B%5D%3D=444");
  }

  /** Tests route {@code /user-list-null} {@link UserController#queryListNullable}. */
  @Test
  public void checkQueryListNullable() {
    var path = "/user-list-null";
    // Correct value
    checkResult(path, "x[]?=333&x[]?=444", okContains("User(333),User(444)"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("null"));
    checkResult(path, "x[]?", okContains("null"));
    checkResult(path, "x[]?=", okContains("null"));
    // Reverse route
    assertThat(UserController.queryListNullable(null).url()).isEqualTo(path);
    assertThat(UserController.queryListNullable(List.of()).url()).isEqualTo(path);
    assertThat(UserController.queryListNullable(testList).url())
        .isEqualTo(path + "?x%5B%5D%3F=333&x%5B%5D%3F=444");
  }

  /** Tests route {@code /user-list-opt} {@link UserController#queryListOptional}. */
  @Test
  public void checkQueryListOptional() {
    var path = "/user-list-opt";
    // Correct value
    checkResult(path, "x[]?=333&x[]?=444", okContains("User(333),User(444)"));
    // Without/Empty/NoValue param
    checkResult(path, this::okEmpty);
    checkResult(path, "x[]?", this::okEmpty);
    checkResult(path, "x[]?=", this::okEmpty);
    // Reverse route
    assertThat(UserController.queryListOptional(Optional.empty()).url()).isEqualTo(path);
    assertThat(UserController.queryListOptional(Optional.of(List.of())).url()).isEqualTo(path);
    assertThat(UserController.queryListOptional(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F=333&x%5B%5D%3F=444");
  }

  /** Tests route {@code /user-list-opt-d} {@link UserController#queryListOptionalDefault}. */
  @Test
  public void checkQueryListOptionalDefault() {
    var path = "/user-list-opt-d";
    // Correct value
    checkResult(path, "x[]?%3D=333&x[]?%3D=444", okContains("User(333),User(444)"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("User(111),User(222)"));
    checkResult(path, "x[]?%3D", okContains("User(111),User(222)"));
    checkResult(path, "x[]?%3D=", okContains("User(111),User(222)"));
    // Reverse route
    assertThat(UserController.queryListOptionalDefault(Optional.empty()).url()).isEqualTo(path);
    assertThat(UserController.queryListOptionalDefault(Optional.of(List.of())).url())
        .isEqualTo(path);
    assertThat(UserController.queryListOptionalDefault(Optional.of(defaultList)).url())
        .isEqualTo(path);
    assertThat(UserController.queryListOptionalDefault(Optional.of(testList)).url())
        .isEqualTo(path + "?x%5B%5D%3F%3D=333&x%5B%5D%3F%3D=444");
  }
}
