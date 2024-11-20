/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static controllers.module.routes.ModuleController;
import static controllers.routes.Application;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.Test;

public class ReverseRoutesTest extends AbstractRoutesTest {

  @Test
  public void checkRoutingOfRoutesIncludes() {
    // Force the router to bootstrap the prefix
    app.injector().instanceOf(play.api.routing.Router.class);
    assertThat(ModuleController.index().url()).isEqualTo("/module/index");
  }

  @Test
  public void checkRouterForMethodWithRequest() {
    assertThat(Application.async(10).url()).isEqualTo("/result/async?x=10");
  }

  @Test
  public void checkOneMethodRoutesWithNonFixedParams() {
    assertThat(
            Application.reverse(
                    false,
                    'x',
                    (short) 789,
                    789,
                    789L,
                    7.89f,
                    7.89,
                    UUIDParamTest.testUUID,
                    OptionalInt.of(789),
                    OptionalLong.of(789),
                    OptionalDouble.of(7.89),
                    "x",
                    Optional.of("x"))
                .url())
        .isEqualTo(
            "/reverse/non-fixed?b=false&c=x&s=789&i=789&l=789&f=7.89&d=7.89&uuid=2ef841cd-0fe0-423c-83ed-71040a1f42fe&oi=789&ol=789&od=7.89&str=x&ostr=x");
  }

  @Test
  public void checkOneMethodRoutesWithFixedParams() {
    assertThat(
            Application.reverse(
                    true,
                    'a',
                    (short) 123,
                    123,
                    123L,
                    1.23f,
                    1.23,
                    UUIDParamTest.defaultUUID,
                    OptionalInt.of(123),
                    OptionalLong.of(123),
                    OptionalDouble.of(1.23),
                    "a",
                    Optional.of("a"))
                .url())
        .isEqualTo("/reverse/fixed");
  }
}
