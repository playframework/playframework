/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

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
}
