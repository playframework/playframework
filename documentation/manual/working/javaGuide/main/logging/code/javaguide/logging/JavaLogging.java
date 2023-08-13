/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.logging;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

// #logging-import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// #logging-import

public class JavaLogging {

  private static final Logger logger = LoggerFactory.getLogger(JavaLogging.class);

  public void testDefaultLogger() {

    // #logging-example
    // Log some debug info
    logger.debug("Attempting risky calculation.");

    try {
      final int result = riskyCalculation();

      // Log result if successful
      logger.debug("Result={}", result);
    } catch (Throwable t) {
      // Log error with message and Throwable.
      logger.error("Exception with riskyCalculation", t);
    }
    // #logging-example

  }

  @Test
  void testCreateLogger() {

    // #logging-create-logger-name
    final Logger accessLogger = LoggerFactory.getLogger("access");
    // #logging-create-logger-name

    assertEquals("access", accessLogger.getName());

    // #logging-create-logger-class
    final Logger log = LoggerFactory.getLogger(this.getClass());
    // #logging-create-logger-class

    assertEquals("javaguide.logging.JavaLogging", log.getName());
  }

  private int riskyCalculation() {
    return 10 / (new Random()).nextInt(2);
  }
}
