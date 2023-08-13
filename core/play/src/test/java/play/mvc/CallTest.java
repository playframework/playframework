/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;

class CallTest {

  @Test
  void calShouldReturnCorrectUrlInPath() {
    final TestCall call = new TestCall("/myurl", "GET");

    assertEquals("/myurl", call.path());
  }

  @Test
  void callShouldReturnCorrectUrlAndFragmentInPath() {
    final Call call = new TestCall("/myurl", "GET").withFragment("myfragment");

    assertEquals("/myurl#myfragment", call.path());
  }

  @Test
  void absoluteURLWithRequestShouldHaveHTTPScheme() {
    final Request req = new RequestBuilder().uri("http://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("http://playframework.com/url", call.absoluteURL(req));
  }

  @Test
  void absoluteURLWithRequestAndSecureParameterIsFalseShouldHaveHTTPScheme() {
    final Request req = new RequestBuilder().uri("https://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("http://playframework.com/url", call.absoluteURL(req, false));
  }

  @Test
  void absoluteURLWithHostAndSecureParameterIsFalseShouldHaveHTTPScheme() {
    final TestCall call = new TestCall("/url", "GET");

    assertEquals("http://typesafe.com/url", call.absoluteURL(false, "typesafe.com"));
  }

  @Test
  void absoluteURLWithRequestShouldHaveHTTPSScheme() {
    final Request req = new RequestBuilder().uri("https://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("https://playframework.com/url", call.absoluteURL(req));
  }

  @Test
  void absoluteUrlWithRequestAndSecureParameterIsTrueShouldHaveHTTPSScheme() {
    final Request req = new RequestBuilder().uri("http://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("https://playframework.com/url", call.absoluteURL(req, true));
  }

  @Test
  void absoluteURLWithHostAndSecureParameterIsTrueShouldHaveHTTPSScheme() {
    final TestCall call = new TestCall("/url", "GET");

    assertEquals("https://typesafe.com/url", call.absoluteURL(true, "typesafe.com"));
  }

  @Test
  void webSocketURLWithRequestShouldHaveHTTPScheme() {
    final Request req = new RequestBuilder().uri("http://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("ws://playframework.com/url", call.webSocketURL(req));
  }

  @Test
  void webSocketURLWithRequestAndSecureParameterIsFalseShouldHaveHTTPScheme() {
    final Request req = new RequestBuilder().uri("https://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("ws://playframework.com/url", call.webSocketURL(req, false));
  }

  @Test
  void webSocketURLWithHostAndSecureParameterIsFalseShouldHaveHTTPScheme() {
    final TestCall call = new TestCall("/url", "GET");

    assertEquals("ws://typesafe.com/url", call.webSocketURL(false, "typesafe.com"));
  }

  @Test
  void webSocketURLWithRequestShouldHaveHTTPSScheme() {
    final Request req = new RequestBuilder().uri("https://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("wss://playframework.com/url", call.webSocketURL(req));
  }

  @Test
  void webSocketURLWithRequestAndSecureParameterIsTrueShouldHaveHTTPSScheme() {
    final Request req = new RequestBuilder().uri("http://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("wss://playframework.com/url", call.webSocketURL(req, true));
  }

  @Test
  void webSocketURLWithHostAndSecureParameterIsTrueShouldHaveHTTPSScheme() {
    final TestCall call = new TestCall("/url", "GET");

    assertEquals("wss://typesafe.com/url", call.webSocketURL(true, "typesafe.com"));
  }

  @Test
  void relativePathTakesStartPathFromRequest() {
    final Request req = new RequestBuilder().uri("http://playframework.com/one/two").build();

    final TestCall call = new TestCall("/one/two-b", "GET");

    assertEquals("two-b", call.relativeTo(req));
  }

  @Test
  void relativePathTakesStartPathAsString() {
    final String startPath = "/one/two";

    final TestCall call = new TestCall("/one/two-b", "GET");

    assertEquals("two-b", call.relativeTo(startPath));
  }

  @Test
  void relativePathIncludesFragment() {
    final Request req = new RequestBuilder().uri("http://playframework.com/one/two").build();

    final TestCall call = new TestCall("/one/two-b", "GET", "foo");

    assertEquals("two-b#foo", call.relativeTo(req));
  }

  @Test
  void canonicalPathReturnedFromCall() {
    final TestCall call = new TestCall("/one/.././two//three-b", "GET");

    assertEquals("/two/three-b", call.canonical());
  }
}

final class TestCall extends Call {
  private final String u;
  private final String m;
  private final String f;

  TestCall(String u, String m) {
    this.u = u;
    this.m = m;
    this.f = null;
  }

  TestCall(String u, String m, String f) {
    this.u = u;
    this.m = m;
    this.f = f;
  }

  public String url() {
    return this.u;
  }

  public String method() {
    return this.m;
  }

  public String fragment() {
    return this.f;
  }
}
