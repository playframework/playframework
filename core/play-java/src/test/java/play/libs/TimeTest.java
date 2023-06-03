/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class TimeTest {

  static final int oneSecond = 1;
  static final int oneMinute = 60;
  static final int oneHour = oneMinute * 60;
  static final int oneDay = oneHour * 24;
  static final int thirtyDays = oneDay * 30;

  @Test
  public void testDefaultTime() {
    int result = Time.parseDuration(null);
    assertEquals(thirtyDays, result);
  }

  @Test
  public void testSeconds() {
    int result1 = Time.parseDuration("1s");
    assertEquals(oneSecond, result1);

    int result2 = Time.parseDuration("100s");
    assertEquals(oneSecond * 100, result2);

    try {
      Time.parseDuration("1S");
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid duration pattern : " + "1S", iae.getMessage());
    }

    try {
      Time.parseDuration("100S");
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid duration pattern : " + "100S", iae.getMessage());
    }
  }

  @Test
  public void testMinutes() {
    int result1 = Time.parseDuration("1mn");
    assertEquals(oneMinute, result1);

    int result2 = Time.parseDuration("100mn");
    assertEquals(oneMinute * 100, result2);

    int result3 = Time.parseDuration("1min");
    assertEquals(oneMinute, result3);

    int result4 = Time.parseDuration("100min");
    assertEquals(oneMinute * 100, result4);

    try {
      Time.parseDuration("1MIN");
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid duration pattern : " + "1MIN", iae.getMessage());
    }

    try {
      Time.parseDuration("100MN");
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid duration pattern : " + "100MN", iae.getMessage());
    }

    try {
      Time.parseDuration("100mN");
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid duration pattern : " + "100mN", iae.getMessage());
    }
  }

  @Test
  public void testHours() {
    int result1 = Time.parseDuration("1h");
    assertEquals(oneHour, result1);

    int result2 = Time.parseDuration("100h");
    assertEquals(oneHour * 100, result2);

    try {
      Time.parseDuration("1H");
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid duration pattern : " + "1H", iae.getMessage());
    }

    try {
      Time.parseDuration("100H");
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid duration pattern : " + "100H", iae.getMessage());
    }
  }

  @Test
  public void testDays() {
    int result1 = Time.parseDuration("1d");
    assertEquals(oneDay, result1);

    int result2 = Time.parseDuration("100d");
    assertEquals(oneDay * 100, result2);

    try {
      Time.parseDuration("1D");
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid duration pattern : " + "1D", iae.getMessage());
    }

    try {
      Time.parseDuration("100D");
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid duration pattern : " + "100D", iae.getMessage());
    }
  }
}
