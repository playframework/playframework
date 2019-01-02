/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.docs.sbtplugin

import sbt._

private[sbtplugin] class PlayDocsValidationCompat {

  def getMarkdownFiles(base: java.io.File): Seq[(File, String)] = {
    (base / "manual" ** "*.md").get.pair(relativeTo(base))
  }
}
