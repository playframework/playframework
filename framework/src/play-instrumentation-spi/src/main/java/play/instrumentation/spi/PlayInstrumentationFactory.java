/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.instrumentation.spi;

/**
 * Provides a factory to return a {@link PlayInstrumentation} instance for each request.
 */
public interface PlayInstrumentationFactory {
  public PlayInstrumentation createPlayInstrumentation();
}
