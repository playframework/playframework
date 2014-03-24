/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.instrumentation.spi;

import java.util.Map;
import java.util.List;

public interface PlayInvoked {
  public String getController();
  public String getMethod();
  public String getPattern();
  public long getId();
  public String getUri();
  public String getPath();
  public String getHttpMethod();
  public String getVersion();
  public String getRemoteAddress();
  public String getHost();
  public String getDomain();
  public Map<String, String> getSession();
}