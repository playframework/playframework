/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport.classloader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Objects;

/** A ClassLoader that only uses resources from its parent */
public class DelegatedResourcesClassLoader extends NamedURLClassLoader {

  public DelegatedResourcesClassLoader(String name, URL[] urls, ClassLoader parent) {
    super(name, urls, parent);
    Objects.requireNonNull(parent);
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    return getParent().getResources(name);
  }
}
