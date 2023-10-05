/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;

public class CallTest {

  @Test
  public void calShouldReturnCorrectUrlInPath() {
    final TestCall call = new TestCall("/myurl", "GET");

    assertEquals("/myurl", call.path());
  }

  @Test
  public void callShouldReturnCorrectUrlAndFragmentInPath() {
    final Call call = new TestCall("/myurl", "GET").withFragment("myfragment");

    assertEquals("/myurl#myfragment", call.path());
  }

  @Test
  public void absoluteURLWithRequestShouldHaveHTTPScheme() {
    final Request req = new RequestBuilder().uri("http://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("http://playframework.com/url", call.absoluteURL(req));
  }

  @Test
  public void absoluteURLWithRequestAndSecureParameterIsFalseShouldHaveHTTPScheme() {
    final Request req = new RequestBuilder().uri("https://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("http://playframework.com/url", call.absoluteURL(req, false));
  }

  @Test
  public void absoluteURLWithHostAndSecureParameterIsFalseShouldHaveHTTPScheme() {
    final TestCall call = new TestCall("/url", "GET");

    assertEquals("http://foobar.com/url", call.absoluteURL(false, "foobar.com"));
  }

  @Test
  public void absoluteURLWithRequestShouldHaveHTTPSScheme() {
    final Request req = new RequestBuilder().uri("https://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("https://playframework.com/url", call.absoluteURL(req));
  }

  @Test
  public void absoluteUrlWithRequestAndSecureParameterIsTrueShouldHaveHTTPSScheme() {
    final Request req = new RequestBuilder().uri("http://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("https://playframework.com/url", call.absoluteURL(req, true));
  }

  @Test
  public void absoluteURLWithHostAndSecureParameterIsTrueShouldHaveHTTPSScheme() {
    final TestCall call = new TestCall("/url", "GET");

    assertEquals("https://foobar.com/url", call.absoluteURL(true, "foobar.com"));
  }

  @Test
  public void webSocketURLWithRequestShouldHaveHTTPScheme() {
    final Request req = new RequestBuilder().uri("http://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("ws://playframework.com/url", call.webSocketURL(req));
  }

  @Test
  public void webSocketURLWithRequestAndSecureParameterIsFalseShouldHaveHTTPScheme() {
    final Request req = new RequestBuilder().uri("https://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("ws://playframework.com/url", call.webSocketURL(req, false));
  }

  @Test
  public void webSocketURLWithHostAndSecureParameterIsFalseShouldHaveHTTPScheme() {
    final TestCall call = new TestCall("/url", "GET");

    assertEquals("ws://foobar.com/url", call.webSocketURL(false, "foobar.com"));
  }

  @Test
  public void webSocketURLWithRequestShouldHaveHTTPSScheme() {
    final Request req = new RequestBuilder().uri("https://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("wss://playframework.com/url", call.webSocketURL(req));
  }

  @Test
  public void webSocketURLWithRequestAndSecureParameterIsTrueShouldHaveHTTPSScheme() {
    final Request req = new RequestBuilder().uri("http://playframework.com/playframework").build();

    final TestCall call = new TestCall("/url", "GET");

    assertEquals("wss://playframework.com/url", call.webSocketURL(req, true));
  }

  @Test
  public void webSocketURLWithHostAndSecureParameterIsTrueShouldHaveHTTPSScheme() {
    final TestCall call = new TestCall("/url", "GET");

    assertEquals("wss://foobar.com/url", call.webSocketURL(true, "foobar.com"));
  }

  @Test
  public void relativePathTakesStartPathFromRequest() {
    final Request req = new RequestBuilder().uri("http://playframework.com/one/two").build();

    final TestCall call = new TestCall("/one/two-b", "GET");

    assertEquals("two-b", call.relativeTo(req));
  }

  @Test
  public void relativePathTakesStartPathAsString() {
    final String startPath = "/one/two";

    final TestCall call = new TestCall("/one/two-b", "GET");

    assertEquals("two-b", call.relativeTo(startPath));
  }

  @Test
  public void relativePathIncludesFragment() {
    final Request req = new RequestBuilder().uri("http://playframework.com/one/two").build();

    final TestCall call = new TestCall("/one/two-b", "GET", "foo");

    assertEquals("two-b#foo", call.relativeTo(req));
  }

  @Test
  public void canonicalPathReturnedFromCall() {
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
