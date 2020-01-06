/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport.classloader;

import java.net.URLClassLoader;

public interface ApplicationClassLoaderProvider {
  URLClassLoader get();
}
