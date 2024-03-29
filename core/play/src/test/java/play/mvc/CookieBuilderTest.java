/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static org.junit.Assert.*;

import org.junit.Test;

public class CookieBuilderTest {

  @Test
  public void createACookieWithNameAndValueAndKeepDefaults() {
    Http.Cookie cookie = Http.Cookie.builder("name", "value").build();
    assertEquals("name", cookie.name());
    assertEquals("value", cookie.value());
    assertEquals("/", cookie.path());
    assertNull(cookie.domain());
    assertNull(cookie.maxAge());
    assertFalse(cookie.secure());
    assertTrue(cookie.httpOnly());
  }

  @Test
  public void createACookieWithNameAndValueAndChangePath() {
    Http.Cookie cookie = Http.Cookie.builder("name", "value").withPath("path1/path").build();
    assertEquals("name", cookie.name());
    assertEquals("value", cookie.value());
    assertEquals("path1/path", cookie.path());
    assertNull(cookie.domain());
    assertNull(cookie.maxAge());
    assertFalse(cookie.secure());
    assertTrue(cookie.httpOnly());
  }

  @Test
  public void createACookieWithNameAndValueAndChangeDomain() {
    Http.Cookie cookie = Http.Cookie.builder("name", "value").withDomain(".example.com").build();
    assertEquals("name", cookie.name());
    assertEquals("value", cookie.value());
    assertEquals("/", cookie.path());
    assertEquals(".example.com", cookie.domain());
    assertNull(cookie.maxAge());
    assertFalse(cookie.secure());
    assertTrue(cookie.httpOnly());
  }

  @Test
  public void createACookieWithNameAndValueWithSecureAndHttpOnlyEqualToTrue() {
    Http.Cookie cookie =
        Http.Cookie.builder("name", "value").withSecure(true).withHttpOnly(true).build();
    assertEquals("name", cookie.name());
    assertEquals("value", cookie.value());
    assertEquals("/", cookie.path());
    assertNull(cookie.domain());
    assertNull(cookie.maxAge());
    assertTrue(cookie.secure());
    assertTrue(cookie.httpOnly());
  }
}
