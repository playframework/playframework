/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.instrumentation.spi;

import java.util.Map;
import java.util.List;

public interface PlayError {
  public String getMessage();
  public List<String> getStackTrace();
}