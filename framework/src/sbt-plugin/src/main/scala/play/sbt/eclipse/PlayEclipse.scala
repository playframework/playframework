/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbt.eclipse

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
    import com.typesafe.sbteclipse.core.{ Validation, _ }

    import scala.xml._
    import scala.xml.transform.RewriteRule

    val `/` = java.io.File.separator

    lazy val addClassesManaged = new EclipseTransformerFactory[RewriteRule] {
      override def createTransformer(ref: ProjectRef, state: State): Validation[RewriteRule] = {
        setting(crossTarget in ref, state) map { ct =>
          new RewriteRule {
            override def transform(node: Node): Seq[Node] = node match {
              case elem if (elem.label == "classpath" && (ct / "classes_managed").exists) =>
                val newChild = elem.child ++
                  <classpathentry path={ (ct / "classes_managed").getAbsolutePath } kind="lib"></classpathentry>
                Elem(elem.prefix, "classpath", elem.attributes, elem.scope, false, newChild: _*)
              case other =>
                other
            }
          }
        }
      }
    }

    scalaIDE match {
      case EclipseProjectFlavor.Scala =>
        EclipsePlugin.eclipseSettings ++ Seq(
          EclipseKeys.projectFlavor := EclipseProjectFlavor.Scala,
          EclipseKeys.withBundledScalaContainers := true,
          EclipseKeys.preTasks := Seq(compile in Compile),
          EclipseKeys.createSrc := EclipseCreateSrc.All
        )
      case EclipseProjectFlavor.Java =>
        generateJavaPrefFile()
        EclipsePlugin.eclipseSettings ++ Seq(
          EclipseKeys.projectFlavor := EclipseProjectFlavor.Java,
          EclipseKeys.withBundledScalaContainers := false,
          EclipseKeys.preTasks := Seq(compile in Compile),
          EclipseKeys.classpathTransformerFactories := Seq(addClassesManaged)
        )
    }
  }
}
