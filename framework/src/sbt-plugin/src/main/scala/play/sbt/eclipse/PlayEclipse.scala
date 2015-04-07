/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbt.eclipse

import com.typesafe.sbteclipse.core.EclipsePlugin
import com.typesafe.sbteclipse.core.EclipsePlugin._
import sbt.Keys._
import sbt._

object PlayEclipse {

  private def generateJavaPrefFile(): Unit = {
    val coreSettings = file(".settings") / "org.eclipse.core.resources.prefs"
    if (!coreSettings.exists) {
      IO.createDirectory(coreSettings.getParentFile)
      IO.write(coreSettings,
        """|eclipse.preferences.version=1
           |encoding/<project>=UTF-8""".stripMargin
      )
    }
  }

  /**
   * provides Settings for the eclipse project
   * @param scalaIDE whether the Scala IDE is being used
   */
  def eclipseCommandSettings(scalaIDE: EclipseProjectFlavor.Value): Seq[Setting[_]] = {
    scalaIDE match {
      case EclipseProjectFlavor.Scala =>
        EclipsePlugin.eclipseSettings ++ Seq(
          EclipseKeys.projectFlavor := EclipseProjectFlavor.Scala,
          EclipseKeys.withBundledScalaContainers := true,
          EclipseKeys.createSrc := EclipseCreateSrc.All
        )
      case EclipseProjectFlavor.Java =>
        generateJavaPrefFile()
        EclipsePlugin.eclipseSettings ++ Seq(
          EclipseKeys.projectFlavor := EclipseProjectFlavor.Java,
          EclipseKeys.withBundledScalaContainers := false,
          EclipseKeys.preTasks := Seq(compile in Compile),
          EclipseKeys.createSrc := EclipseCreateSrc.All
        )
    }
  }
}
