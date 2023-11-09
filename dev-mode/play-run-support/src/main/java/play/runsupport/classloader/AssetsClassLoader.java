/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport.classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;

/**
 * A ClassLoader for serving assets. Serves assets from the given directories, at the given prefix.
 */
public class AssetsClassLoader extends ClassLoader {

  /** An assets directories, mapped by the prefix they should be served from. */
  private final List<Entry<String, File>> assets;

  public AssetsClassLoader(ClassLoader parent, List<Entry<String, File>> assets) {
    super(parent);
    this.assets = assets;
  }

  @Override
  public URL findResource(String name) {
    return assets.stream()
        .filter(e -> exists(name, e.getKey(), e.getValue()))
        .findFirst()
        .map(
            e -> {
              try {
                return new File(e.getValue(), name.substring(e.getKey().length())).toURI().toURL();
              } catch (MalformedURLException ex) {
                return null;
              }
            })
        .orElse(null);
  }

  private boolean exists(String name, String prefix, File dir) {
    return name.startsWith(prefix) && new File(dir, name.substring(prefix.length())).isFile();
  }
}
