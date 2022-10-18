/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport.classloader;

import java.net.URLClassLoader;

public interface ApplicationClassLoaderProvider {
  URLClassLoader get();
}
