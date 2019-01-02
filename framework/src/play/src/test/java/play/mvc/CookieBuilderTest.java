/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import org.junit.Test;

import static org.junit.Assert.*;

public class CookieBuilderTest {

  @Test
  public void createACookieWithNameAndValueAndKeepDefaults() {
    Http.Cookie cookie =  Http.Cookie.builder("name", "value").build();
    assertEquals("name", cookie.name());
    assertEquals("value", cookie.value());
    assertEquals("/", cookie.path());
    assertEquals(null, cookie.domain());
    assertEquals(null, cookie.maxAge());
    assertEquals(false, cookie.secure());
    assertEquals(true, cookie.httpOnly());
  }

  @Test
  public void createACookieWithNameAndValueAndChangePath() {
    Http.Cookie cookie =  Http.Cookie.builder("name", "value").withPath("path1/path").build();
    assertEquals("name", cookie.name());
    assertEquals("value", cookie.value());
    assertEquals("path1/path", cookie.path());
    assertEquals(null, cookie.domain());
    assertEquals(null, cookie.maxAge());
    assertEquals(false, cookie.secure());
    assertEquals(true, cookie.httpOnly());
  }

  @Test
  public void createACookieWithNameAndValueAndChangeDomain() {
    Http.Cookie cookie =  Http.Cookie.builder("name", "value").withDomain(".example.com").build();
    assertEquals("name", cookie.name());
    assertEquals("value", cookie.value());
    assertEquals("/", cookie.path());
    assertEquals(".example.com", cookie.domain());
    assertEquals(null, cookie.maxAge());
    assertEquals(false, cookie.secure());
    assertEquals(true, cookie.httpOnly());
  }

  @Test
  public void createACookieWithNameAndValueWithSecureAndHttpOnlyEqualToTrue() {
    Http.Cookie cookie =  Http.Cookie.builder("name", "value").withSecure(true).withHttpOnly(true).build();
    assertEquals("name", cookie.name());
    assertEquals("value", cookie.value());
    assertEquals("/", cookie.path());
    assertEquals(null, cookie.domain());
    assertEquals(null, cookie.maxAge());
    assertEquals(true, cookie.secure());
    assertEquals(true, cookie.httpOnly());
  }

}
