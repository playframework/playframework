/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.HttpVerbs.DELETE;
import static play.mvc.Http.HttpVerbs.HEAD;
import static play.mvc.Http.HttpVerbs.OPTIONS;
import static play.mvc.Http.HttpVerbs.PATCH;
import static play.mvc.Http.HttpVerbs.PUT;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.HttpVerbs.GET;
import static play.mvc.Http.HttpVerbs.POST;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

public class MethodTest extends AbstractRoutesTest {

  @Test
  public void checkGet() {
    var result = route(app, fakeRequest(GET, "/method/get"));
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).isEqualTo("GET");
    result = route(app, fakeRequest(POST, "/method/get"));
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void checkPost() {
    var result = route(app, fakeRequest(POST, "/method/post"));
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).isEqualTo("POST");
    result = route(app, fakeRequest(GET, "/method/post"));
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void checkPut() {
    var result = route(app, fakeRequest(PUT, "/method/put"));
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).isEqualTo("PUT");
    result = route(app, fakeRequest(GET, "/method/put"));
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void checkPatch() {
    var result = route(app, fakeRequest(PATCH, "/method/patch"));
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).isEqualTo("PATCH");
    result = route(app, fakeRequest(GET, "/method/patch"));
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void checkDelete() {
    var result = route(app, fakeRequest(DELETE, "/method/delete"));
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).isEqualTo("DELETE");
    result = route(app, fakeRequest(GET, "/method/delete"));
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void checkHead() {
    var result = route(app, fakeRequest(HEAD, "/method/head"));
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).isEqualTo("HEAD");
    result = route(app, fakeRequest(GET, "/method/head"));
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void checkOptions() {
    var result = route(app, fakeRequest(OPTIONS, "/method/options"));
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).isEqualTo("OPTIONS");
    result = route(app, fakeRequest(GET, "/method/options"));
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }
}
