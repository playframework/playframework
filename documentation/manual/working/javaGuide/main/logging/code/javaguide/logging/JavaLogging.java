/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.logging;

import java.util.Random;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;

//#logging-import
import play.Logger;
//#logging-import

public class JavaLogging {

  public void testDefaultLogger() {
      
    //#logging-default-logger
    // Log some debug info
    Logger.debug("Attempting risky calculation.");
      
    try {
      final int result = riskyCalculation();
        
      // Log result if successful
      Logger.debug("Result=" + result);
    } catch (Throwable t) {
      // Log error with message and Throwable.
      Logger.error("Exception with riskyCalculation", t);
    }
    //#logging-default-logger
      
    assertThat(Logger.underlying().getName(), equalTo("application"));
  }
  
  @Test
  public void testCreateLogger() {
    
    //#logging-create-logger-name
    final Logger.ALogger accessLogger = Logger.of("access");
    //#logging-create-logger-name
    
    assertThat(accessLogger.underlying().getName(), equalTo("access"));
    
    //#logging-create-logger-class
    final Logger.ALogger logger = Logger.of(this.getClass());
    //#logging-create-logger-class
    
    assertThat(logger.underlying().getName(), equalTo("javaguide.logging.JavaLogging"));
  }
  
  private int riskyCalculation() {
    return  10 / (new Random()).nextInt(2);
  }
}
