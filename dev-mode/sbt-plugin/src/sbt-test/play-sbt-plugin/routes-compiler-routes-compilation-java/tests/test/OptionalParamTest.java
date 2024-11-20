/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static controllers.routes.OptionalController;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import controllers.OptionalController;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.Test;

public class OptionalParamTest extends AbstractRoutesTest {

  /** Tests route {@code /opt-int} {@link OptionalController#queryInt}. */
  @Test
  public void checkQueryInt() {
    var path = "/opt-int";
    // Correct value
    checkResult(path, "x?=789", okContains("789"));
    checkResult(path, "x?=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("emptyOptionalInt"));
    checkResult(path, "x?", okContains("emptyOptionalInt"));
    checkResult(path, "x?=", okContains("emptyOptionalInt"));
    // Incorrect value
    checkResult(path, "x?=3000000000", this::badRequestCannotParseParameter);
    checkResult(path, "x?=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThatThrownBy(() -> OptionalController.queryInt(null).url())
        .isInstanceOf(NullPointerException.class);
    assertThat(OptionalController.queryInt(OptionalInt.empty()).url()).isEqualTo(path);
    assertThat(OptionalController.queryInt(OptionalInt.of(789)).url())
        .isEqualTo(path + "?x%3F=789");
    assertThat(OptionalController.queryInt(OptionalInt.of(-789)).url())
        .isEqualTo(path + "?x%3F=-789");
  }

  /** Tests route {@code /opt-int-d} {@link OptionalController#queryIntDefault}. */
  @Test
  public void checkQueryIntDefault() {
    var path = "/opt-int-d";
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
    assertThatThrownBy(() -> OptionalController.queryInt(null).url())
        .isInstanceOf(NullPointerException.class);
    assertThat(OptionalController.queryIntDefault(OptionalInt.empty()).url()).isEqualTo(path);
    assertThat(OptionalController.queryIntDefault(OptionalInt.of(123)).url()).isEqualTo(path);
    assertThat(OptionalController.queryIntDefault(OptionalInt.of(789)).url())
        .isEqualTo(path + "?x%3F%3D=789");
    assertThat(OptionalController.queryIntDefault(OptionalInt.of(-789)).url())
        .isEqualTo(path + "?x%3F%3D=-789");
  }

  /** Tests route {@code /opt-long} {@link OptionalController#queryLong}. */
  @Test
  public void checkQueryLong() {
    var path = "/opt-long";
    // Correct value
    checkResult(path, "x?=789", okContains("789"));
    checkResult(path, "x?=-789", okContains("-789"));
    // Without/Empty/NoValue param
    checkResult(path, okContains("emptyOptionalLong"));
    checkResult(path, "x?", okContains("emptyOptionalLong"));
    checkResult(path, "x?=", okContains("emptyOptionalLong"));
    // Incorrect value
    checkResult(
        path, "x?=10000000000000000000", this::badRequestCannotParseParameter); // 10^19 > 2^63
    checkResult(path, "x?=1.0", this::badRequestCannotParseParameter);
    checkResult(path, "x?=invalid", this::badRequestCannotParseParameter);
    // Reverse route
    assertThatThrownBy(() -> OptionalController.queryLong(null).url())
        .isInstanceOf(NullPointerException.class);
    assertThat(OptionalController.queryLong(OptionalLong.empty()).url()).isEqualTo(path);
    assertThat(OptionalController.queryLong(OptionalLong.of(789)).url())
        .isEqualTo(path + "?x%3F=789");
    assertThat(OptionalController.queryLong(OptionalLong.of(-789)).url())
        .isEqualTo(path + "?x%3F=-789");
  }

  /** Tests route {@code /opt-long-d} {@link OptionalController#queryLongDefault}. */
  @Test
  public void checkQueryLongDefault() {
    var path = "/opt-long-d";
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
    assertThatThrownBy(() -> OptionalController.queryLongDefault(null).url())
        .isInstanceOf(NullPointerException.class);
    assertThat(OptionalController.queryLongDefault(OptionalLong.empty()).url()).isEqualTo(path);
    assertThat(OptionalController.queryLongDefault(OptionalLong.of(123)).url()).isEqualTo(path);
    assertThat(OptionalController.queryLongDefault(OptionalLong.of(789)).url())
        .isEqualTo(path + "?x%3F%3D=789");
    assertThat(OptionalController.queryLongDefault(OptionalLong.of(-789)).url())
        .isEqualTo(path + "?x%3F%3D=-789");
  }
}
