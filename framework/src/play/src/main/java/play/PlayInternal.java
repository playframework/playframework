/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play;

/**
 * Play internal global API.
 */
public class PlayInternal {
  /*
   * Global logger for Play internals.
   */
  public static org.slf4j.Logger logger() {
    return play.api.Play$.MODULE$.logger().underlyingLogger();
  }
}
