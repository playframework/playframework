/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.runsupport.classloader;

public interface ApplicationClassLoaderProvider {
  ClassLoader get();
}
