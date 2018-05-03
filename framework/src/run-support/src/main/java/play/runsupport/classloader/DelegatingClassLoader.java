/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport.classloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class DelegatingClassLoader extends ClassLoader {

  private List<String> sharedClasses;
  private ClassLoader buildLoader;
  private ApplicationClassLoaderProvider applicationClassLoaderProvider;

  public DelegatingClassLoader(ClassLoader commonLoader, List<String> sharedClasses, ClassLoader buildLoader, ApplicationClassLoaderProvider applicationClassLoaderProvider) {
    super(commonLoader);
    this.sharedClasses = sharedClasses;
    this.buildLoader = buildLoader;
    this.applicationClassLoaderProvider = applicationClassLoaderProvider;
  }

  @Override
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (sharedClasses.contains(name)) {
      return buildLoader.loadClass(name);
    } else {
      return super.loadClass(name, resolve);
    }
  }

  @Override
  public URL getResource(String name) {
    URLClassLoader appClassLoader = applicationClassLoaderProvider.get();
    URL resource = null;
    if (appClassLoader != null) {
      resource = appClassLoader.findResource(name);
    }
    return resource != null ? resource : super.getResource(name);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    URLClassLoader appClassLoader = applicationClassLoaderProvider.get();
    Enumeration<URL> resources1;
    if (appClassLoader != null) {
      resources1 = appClassLoader.findResources(name);
    } else {
      resources1 = new Vector<URL>().elements();
    }
    Enumeration<URL> resources2 = super.getResources(name);
    return combineResources(resources1, resources2);
  }

  private Enumeration<URL> combineResources(Enumeration<URL> resources1, Enumeration<URL> resources2) {
    Set<URL> set = new HashSet<URL>();
    while (resources1.hasMoreElements()) {
      set.add(resources1.nextElement());
    }
    while (resources2.hasMoreElements()) {
      set.add(resources2.nextElement());
    }
    return new Vector<URL>(set).elements();
  }

  @Override
  public String toString() {
    return "DelegatingClassLoader, using parent: " + getParent();
  }

}
