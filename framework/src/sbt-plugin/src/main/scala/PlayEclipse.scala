package play

import sbt.{ Project => SbtProject, _ }
import sbt.Keys._
import Keys._

import java.io.{ FileWriter, Writer }
import java.util.Properties

trait PlayEclipse {
  this: PlayCommands =>

  private def generateJavaPrefFile(mainLang: String): Unit = {
    val settingsDir = new File(".settings")
    val coreSettings = new File(settingsDir.toString + java.io.File.separator + "org.eclipse.core.resources.prefs")
    if (mainLang == JAVA && coreSettings.exists == false) {
      IO.createDirectory(settingsDir)
      IO.write(coreSettings,
        """|eclipse.preferences.version=1
           |encoding/<project>=UTF-8""".stripMargin
      )
    }
  }

  /**
   * provides Settings for the eclipse project
   * @param mainLang mainly scala or java?
   */
  def eclipseCommandSettings(mainLang: String) = {
    import com.typesafe.sbteclipse.core._
    import com.typesafe.sbteclipse.core.EclipsePlugin._
    import com.typesafe.sbteclipse.core.Validation
    import scala.xml._
    import scala.xml.transform.RewriteRule

    val f = java.io.File.separator

    def err(node: Node) = throw new RuntimeException("error proccessing " + Node)

    lazy val addClassesManaged = new EclipseTransformerFactory[RewriteRule] {
      override def createTransformer(ref: ProjectRef, state: State): Validation[RewriteRule] = {
        setting(crossTarget in ref, state) map { ct =>
          new RewriteRule {
            override def transform(node: Node): Seq[Node] = node match {
              //add classes_managed  
              case elem if (elem.label == "classpathentry" &&
                elem.attribute("path").getOrElse(err(elem)).toString.contains("org.scala-ide.sdt.launching.SCALA_CONTAINER") &&
                new java.io.File(ct + f + "classes_managed").exists) =>
                <classpathentry path={ ct + f + "classes_managed" } kind="lib"></classpathentry>
              case other =>
                other
            }
          }
        }
      }
    }

    lazy val addJavaBuilder = new EclipseTransformerFactory[RewriteRule] {
      override def createTransformer(ref: ProjectRef, state: State): Validation[RewriteRule] = {
        setting(crossTarget in ref, state) map { ct =>
          new RewriteRule {
            override def transform(node: Node): Seq[Node] = node match {
              //add javabuilder
              case elem if (elem.text == "org.scala-ide.sdt.core.scalabuilder") =>
                <name>org.eclipse.jdt.core.javabuilder</name>
              //remove scala nature   
              case elem if (elem.text == "org.scala-ide.sdt.core.scalanature") =>
                <name></name>
              case other =>
                other
            }
          }
        }
      }
    }

    lazy val addScalaLib = new EclipseTransformerFactory[RewriteRule] {
      override def createTransformer(ref: ProjectRef, state: State): Validation[RewriteRule] = {
        evaluateTask(dependencyClasspath in Runtime, ref, state) map { classpath =>
          val scalaLib =
            classpath.find(_.get(moduleID.key).exists(moduleFilter(organization = "org.scala-lang", name = "scala-library"))).map(_.data.getAbsolutePath)
              .getOrElse(throw new RuntimeException("could not find scala-library.jar"))
          new RewriteRule {
            override def transform(node: Node): Seq[Node] = node match {
              //add scala-library.jar
              case elem if (elem.label == "classpath") =>
                val newChild = elem.child ++ <classpathentry path={ scalaLib } kind="lib"></classpathentry>
                Elem(elem.prefix, "classpath", elem.attributes, elem.scope, newChild: _*)
              case other =>
                other
            }
          }
        }
      }
    }

    lazy val addSourcesManaged = new EclipseTransformerFactory[RewriteRule] {
      override def createTransformer(ref: ProjectRef, state: State): Validation[RewriteRule] = {
        setting(crossTarget in ref, state) map { ct =>
          new RewriteRule {
            override def transform(node: Node): Seq[Node] = node match {
              //add src_managed/main
              case elem if (elem.label == "classpath" && new java.io.File(ct + f + "src_managed" + f + "main").exists) =>
                val newChild = elem.child ++ <classpathentry path={ "target" + f + ct.getName + f + "src_managed" + f + "main" } kind="src"></classpathentry>
                Elem(elem.prefix, "classpath", elem.attributes, elem.scope, newChild: _*)
              case other =>
                other
            }
          }
        }
      }
    }

    //setup project file
    val projectTransformers = if (mainLang == SCALA) Seq[EclipseTransformerFactory[RewriteRule]]() else Seq(addJavaBuilder)

    //setup classpath
    val classPathTransformers = if (mainLang == SCALA) Seq(addSourcesManaged) else Seq(addClassesManaged, addScalaLib)

    //generate JDT pref file if needed
    generateJavaPrefFile(mainLang)

    val flavor = if (mainLang == SCALA) EclipseProjectFlavor.Scala else EclipseProjectFlavor.Java

    //setup sbteclipse
    EclipsePlugin.eclipseSettings ++ Seq(
      EclipseKeys.createSrc := EclipseCreateSrc.Default,
      EclipseKeys.eclipseOutput := Some(".target"),
      EclipseKeys.projectFlavor := flavor,
      EclipseKeys.preTasks := Seq(compile in Compile) ++ Seq(scalaIdePlay2Prefs),
      EclipseKeys.projectTransformerFactories := projectTransformers,
      EclipseKeys.classpathTransformerFactories := classPathTransformers)
  }
}

object PlayEclipse {

  def saveScalaIdePlay2Prefs(ref: ProjectRef, structure: Load.BuildStructure, baseDir: File) = {
    def fileWriterMkdirs(file: File): FileWriter = {
      file.getParentFile.mkdirs()
      new FileWriter(file)
    }

    def saveProperties(file: File, settings: Seq[(String, String)]): Unit =
      if (!settings.isEmpty) {
        val properties = new Properties
        for ((key, value) <- settings) properties.setProperty(key, value)
        val writer = fileWriterMkdirs(file);
        try {
          properties.store(writer, "Generated by sbt-plugin")
        } finally {
          writer.close();
        }
      }

    SbtProject.getProject(ref, structure).foreach { p =>
      (templatesImport in ref get structure.data).foreach { imports =>
        val value = imports.mkString("import ", "\nimport ", "\n")
        val properties = Seq(("eclipse.preferences.version", "1"), ("templateImports", value))
        saveProperties(baseDir / ".settings" / "org.scala-ide.play2.prefs", properties)
      }
    }
  }
}
