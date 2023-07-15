/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CookieBuilderTest {

  @Test
  void createACookieWithNameAndValueAndKeepDefaults() {
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
  void createACookieWithNameAndValueAndChangePath() {
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
  void createACookieWithNameAndValueAndChangeDomain() {
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
  void createACookieWithNameAndValueWithSecureAndHttpOnlyEqualToTrue() {
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
