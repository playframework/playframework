/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport.classloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

/** A ClassLoader with a toString() that prints name/urls. */
public class NamedURLClassLoader extends URLClassLoader {

  private final String name;

  public NamedURLClassLoader(String name, URL[] urls, ClassLoader parent) {
    super(urls, parent);
    this.name = name;
  }

  @Override
  public String toString() {
    return name + Arrays.toString(getURLs());
  }
}
