/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.cors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.function.Predicate;
import org.junit.Test;

public class CORSConfigTest {

  @Test
  public void shouldAdaptJavaPredicates() {
    Predicate<String> origins = origin -> origin.equals("https://allowed.example");
    Predicate<String> methods = method -> method.equals("QUERY");
    Predicate<String> headers = header -> header.equalsIgnoreCase("X-Allowed");

    CORSConfig config =
        CORSConfig.denyAll()
            .withOriginsAllowed(origins)
            .withMethodsAllowed(methods)
            .withHeadersAllowed(headers);

    assertTrue(config.allowedForOrigin("https://allowed.example").isDefined());
    assertFalse(config.allowedForOrigin("https://denied.example").isDefined());
    assertEquals(true, config.isHttpMethodAllowed().apply("QUERY"));
    assertEquals(false, config.isHttpMethodAllowed().apply("DELETE"));
    assertEquals(true, config.isHttpHeaderAllowed().apply("x-allowed"));
    assertEquals(false, config.isHttpHeaderAllowed().apply("X-Denied"));
  }
}
