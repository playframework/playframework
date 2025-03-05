/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class DocumentationTest extends AbstractRoutesTest {

  @Test
  public void checkDocumentation() {
    // The purpose of this test is to alert anyone that changes the format of the router
    // documentation that it is being used by OpenAPI.
    var someRoute =
        app.injector()
            .instanceOf(play.api.routing.Router.class)
            .documentation()
            .find((r) -> r._1().equals("GET") && r._2().startsWith("/bool-p/"));
    assertThat(someRoute.isDefined()).isTrue();
    var route = someRoute.get();
    assertThat(route._2()).isEqualTo("/bool-p/$x<[^/]+>");
    assertThat(route._3()).startsWith("controllers.BooleanController.path");
  }
}
