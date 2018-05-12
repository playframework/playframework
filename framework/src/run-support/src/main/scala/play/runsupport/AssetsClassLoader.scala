/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport

import java.io.File

/**
 * A ClassLoader for serving assets.
 *
 * Serves assets from the given directories, at the given prefix.
 *
 * @param assets A list of assets directories, paired with the prefix they should be served from.
 */
class AssetsClassLoader(parent: ClassLoader, assets: Seq[(String, File)]) extends ClassLoader(parent) {
  override def findResource(name: String) = {
    assets.collectFirst {
      case (prefix, dir) if exists(name, prefix, dir) =>
        new File(dir, name.substring(prefix.length)).toURI.toURL
    }.orNull
  }

  def exists(name: String, prefix: String, dir: File) = {
    name.startsWith(prefix) && new File(dir, name.substring(prefix.length)).isFile
  }
}
