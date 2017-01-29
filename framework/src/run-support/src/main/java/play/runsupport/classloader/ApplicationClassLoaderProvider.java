/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.runsupport.classloader;

public interface ApplicationClassLoaderProvider {
  ClassLoader get();
}
