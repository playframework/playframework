/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.test.junit5;

// #test-simple

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class SimpleTest {

  @Test
  void testSum() {
    int a = 1 + 1;
    assertEquals(2, a);
  }

  @Test
  void testString() {
    String str = "Hello world";
    assertFalse(str.isEmpty());
  }
}
// #test-simple
