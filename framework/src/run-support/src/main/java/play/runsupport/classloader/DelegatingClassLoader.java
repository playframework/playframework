/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.runsupport.classloader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

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
    // -- Delegate resource loading. We have to hack here because the default implementation is already recursive.
    Method findResource;
    try {
      findResource = ClassLoader.class.getDeclaredMethod("findResource", String.class);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
    findResource.setAccessible(true);
    ClassLoader appClassLoader = applicationClassLoaderProvider.get();
    URL resource = null;
    if (appClassLoader != null) {
      try {
        resource = (URL) findResource.invoke(appClassLoader, name);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException(e);
      } catch (InvocationTargetException e) {
        throw new IllegalStateException(e);
      }
    }
    return resource != null ? resource : super.getResource(name);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    Method findResources;
    try {
      findResources = ClassLoader.class.getDeclaredMethod("findResources", String.class);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
    findResources.setAccessible(true);
    ClassLoader appClassLoader = applicationClassLoaderProvider.get();
    Enumeration<URL> resources1;
    if (appClassLoader != null) {
      try {
        resources1 = (Enumeration<URL>) findResources.invoke(appClassLoader, name);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException(e);
      } catch (InvocationTargetException e) {
        throw new IllegalStateException(e);
      }
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
