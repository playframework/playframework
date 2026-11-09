/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public class JavaReverseRouterTest {
  @Test
  public void rejectUnmatchedReverseRouteArgumentsWithIllegalArgumentException() {
    assertThatThrownBy(() -> controllers.routes.Application.fixedOnlyParams("other"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "No route for controllers.Application.fixedOnlyParams matches the arguments [other]");
  }

  @Test
  public void leaveOutTrailingParametersWithDefaultValues() {
    assertThat(controllers.routes.BooleanController.queryDefault().url()).isEqualTo("/bool-d");
    assertThat(controllers.routes.BooleanController.queryDefault(false).url())
        .isEqualTo("/bool-d?x%3F%3D=false");
  }
}
