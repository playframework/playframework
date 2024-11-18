/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static controllers.routes.Application;
import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.GET;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

import org.junit.Test;

public class BooleanParamTest extends AbstractRoutesTest {

  @Test
  public void checkBindFromQueryString() {
    var result = route(app, fakeRequest(GET, "/take-bool?b=true"));
    assertThat(contentAsString(result)).isEqualTo("true");
    var result2 = route(app, fakeRequest(GET, "/take-bool?b=false"));
    assertThat(contentAsString(result2)).isEqualTo("false");
    // Bind boolean values from 1 and 0 integers too
    assertThat(contentAsString(route(app, fakeRequest(GET, "/take-bool?b=1")))).isEqualTo("true");
    assertThat(contentAsString(route(app, fakeRequest(GET, "/take-bool?b=0")))).isEqualTo("false");
  }

  @Test
  public void checkBindFromPath() {
    var result = route(app, fakeRequest(GET, "/take-bool-2/true"));
    assertThat(contentAsString(result)).isEqualTo("true");
    var result2 = route(app, fakeRequest(GET, "/take-bool-2/false"));
    assertThat(contentAsString(result2)).isEqualTo("false");
    // Bind boolean values from 1 and 0 integers too
    assertThat(contentAsString(route(app, fakeRequest(GET, "/take-bool-2/1")))).isEqualTo("true");
    assertThat(contentAsString(route(app, fakeRequest(GET, "/take-bool-2/0")))).isEqualTo("false");
  }

  @Test
  public void checkReverseInQuery() {
    assertThat(Application.takeBool(true).url()).isEqualTo("/take-bool?b=true");
    assertThat(Application.takeBool(false).url()).isEqualTo("/take-bool?b=false");
  }

  @Test
  public void checkReverseInPath() {
    assertThat(Application.takeBool2(true).url()).isEqualTo("/take-bool-2/true");
    assertThat(Application.takeBool2(false).url()).isEqualTo("/take-bool-2/false");
  }

}
