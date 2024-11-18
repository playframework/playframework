/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static controllers.module.routes.ModuleController;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ReverseRoutesTest extends AbstractRoutesTest {

  @Test
  public void checkRoutingOfRoutesIncludes() {
    // Force the router to bootstrap the prefix
    app.injector().instanceOf(play.api.routing.Router.class);
    assertThat(ModuleController.index().url()).isEqualTo("/module/index");
  }
}
