import play.console.Colors
import play.core.server.ServerWithStop
import play.markdown._
import sbt._
import Keys._
import PlayKeys._
import play.core.{SBTLink, PlayVersion}
import PlaySourceGenerators._

object ApplicationBuild extends Build {

  lazy val main = Project("Play-Documentation", file(".")).settings(
    version := PlayVersion.current,
    scalaVersion := PlayVersion.scalaVersion,
    libraryDependencies ++= Seq(
      component("play") % "test",
      component("play-test") % "test",
      component("play-java") % "test",
      component("play-cache") % "test"
    ),

    javaManualSourceDirectories <<= (baseDirectory)(base => (base / "manual" / "javaGuide" ** "code").get),
    scalaManualSourceDirectories <<= (baseDirectory)(base => (base / "manual" / "scalaGuide" ** "code").get),

    unmanagedSourceDirectories in Test <++= javaManualSourceDirectories,
    unmanagedSourceDirectories in Test <++= scalaManualSourceDirectories,
    unmanagedSourceDirectories in Test <++= (baseDirectory)(base => (base / "manual" / "detailledTopics" ** "code").get),

    unmanagedResourceDirectories in Test <++= javaManualSourceDirectories,
    unmanagedResourceDirectories in Test <++= scalaManualSourceDirectories,
    unmanagedResourceDirectories in Test <++= (baseDirectory)(base => (base / "manual" / "detailledTopics" ** "code").get),

    parallelExecution in Test := false,

    (compile in Test) <<= Enhancement.enhanceJavaClasses,

    javacOptions ++= Seq("-g", "-Xlint:deprecation"),

    // Need to ensure that templates in the Java docs get Java imports, and in the Scala docs get Scala imports
    sourceGenerators in Test <+= (state, javaManualSourceDirectories, sourceManaged in Test, templatesTypes) map { (s, ds, g, t) =>
      ds.flatMap(d => ScalaTemplates(s, d, g, t, defaultTemplatesImport ++ defaultJavaTemplatesImport))
    },
    sourceGenerators in Test <+= (state, scalaManualSourceDirectories, sourceManaged in Test, templatesTypes) map { (s, ds, g, t) =>
      ds.flatMap(d => ScalaTemplates(s, d, g, t, defaultTemplatesImport ++ defaultScalaTemplatesImport))
    },

    sourceGenerators in Test <+= (state, javaManualSourceDirectories, sourceManaged in Test) map  { (s, ds, g) =>
      ds.flatMap(d => RouteFiles(s, d, g, Seq("play.libs.F"), true, true))
    },
    sourceGenerators in Test <+= (state, scalaManualSourceDirectories, sourceManaged in Test) map  { (s, ds, g) =>
      ds.flatMap(d => RouteFiles(s, d, g, Seq(), true, true))
    },

    templatesTypes := Map(
      "html" -> "play.api.templates.HtmlFormat"
    ),

    run <<= docsRunSetting,

    DocValidation.validateDocs <<= DocValidation.ValidateDocsTask

  )

  // Run a documentation server
  val docsRunSetting: Project.Initialize[InputTask[Unit]] = inputTask { (argsTask: TaskKey[Seq[String]]) =>
    (argsTask, state) map { (args, state) =>
      val extracted = Project.extract(state)

      val port = args.headOption.map(_.toInt).getOrElse(9000)

      // Get classloader
      val sbtLoader = this.getClass.getClassLoader
      Project.runTask(dependencyClasspath in Test, state).get._2.toEither.right.map { classpath: Seq[Attributed[File]] =>
        val classloader = new java.net.URLClassLoader(classpath.map(_.data.toURI.toURL).toArray, null /* important here, don't depend of the sbt classLoader! */) {
          val sharedClasses = Seq(
            classOf[play.core.SBTLink].getName,
            classOf[play.core.server.ServerWithStop].getName,
            classOf[play.api.UsefulException].getName,
            classOf[play.api.PlayException].getName,
            classOf[play.api.PlayException.InterestingLines].getName,
            classOf[play.api.PlayException.RichDescription].getName,
            classOf[play.api.PlayException.ExceptionSource].getName,
            classOf[play.api.PlayException.ExceptionAttachment].getName)

          override def loadClass(name: String): Class[_] = {
            if (sharedClasses.contains(name)) {
              sbtLoader.loadClass(name)
            } else {
              super.loadClass(name)
            }
          }
        }

        import scala.collection.JavaConverters._
        // create sbt link
        val sbtLink = new SBTLink with MarkdownSupport {
          def runTask(name: String) = null
          def reload() = null
          def projectPath() = extracted.currentProject.base
          def settings() = Map.empty[String, String].asJava
          def forceReload() {}
          def findSource(className: String, line: java.lang.Integer) = null
        }

        val clazz = classloader.loadClass("play.core.system.DocumentationServer")
        val constructor = clazz.getConstructor(classOf[SBTLink], classOf[java.lang.Integer])
        val server = constructor.newInstance(sbtLink, new java.lang.Integer(port)).asInstanceOf[ServerWithStop]

        println()
        println(Colors.green("Documentation server started, you can now view the docs by going to http://localhost:" + port))
        println()

        waitForKey()

        server.stop()
      }

      state
    }
  }

  private val consoleReader = new jline.ConsoleReader

  private def waitForKey() = {
    consoleReader.getTerminal.disableEcho()
    def waitEOF() {
      consoleReader.readVirtualKey() match {
        case 4 => // STOP
        case 11 => consoleReader.clearScreen(); waitEOF()
        case 10 => println(); waitEOF()
        case _ => waitEOF()
      }

    }
    waitEOF()
    consoleReader.getTerminal.enableEcho()
  }

  lazy val javaManualSourceDirectories = SettingKey[Seq[File]]("java-manual-source-directories")
  lazy val scalaManualSourceDirectories = SettingKey[Seq[File]]("scala-manual-source-directories")

}
