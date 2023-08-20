/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport

import java.net.URL

/**
 * A ClassLoader that only uses resources from its parent
 */
class DelegatedResourcesClassLoader(name: String, urls: Array[URL], parent: ClassLoader)
    extends NamedURLClassLoader(name, urls, parent) {
  require(parent ne null)
  override def getResources(name: String): java.util.Enumeration[java.net.URL] = getParent.getResources(name)
}
