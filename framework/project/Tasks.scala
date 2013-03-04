import sbt._
import Keys._

object Generators {

  val PlayVersion = {
    dir: File =>
      val file = dir / "PlayVersion.scala"
      IO.write(file,
        """|package play.core
            |
            |object PlayVersion {
            |    val current = "%s"
            |    val scalaVersion = "%s"
            |}
          """.stripMargin.format(BuildSettings.buildVersion, BuildSettings.buildScalaVersion))
      Seq(file)
  }

}

object Tasks {

  import BuildSettings._

  // ----- Reset repo

  val resetRepository = TaskKey[File]("reset-repository")
  val resetRepositoryTask = resetRepository := {
    val repository = file("../repository/local")
    IO.createDirectory(repository)
    repository
  }

  // ----- Generate API docs

  val generateAPIDocs = TaskKey[Unit]("api-docs")
  val generateAPIDocsTask = TaskKey[Unit]("api-docs") <<= (dependencyClasspath in Test, compilers, streams, baseDirectory, scalaBinaryVersion) map {
    (classpath, cs, s, base, sbv) =>

      val allJars = (file("src") ** "*.jar").get

      IO.delete(file("../documentation/api"))

      // Scaladoc
      val sourceFiles =
        (file("src/play/src/main/scala/play/api") ** "*.scala").get ++
          (file("src/iteratees/src/main/scala") ** "*.scala").get ++
          (file("src/play-test/src/main/scala") ** "*.scala").get ++
          (file("src/play/src/main/scala/views") ** "*.scala").get ++
          (file("src/anorm/src/main/scala") ** "*.scala").get ++
          (file("src/play-filters-helpers/src/main/scala") ** "*.scala").get ++
          (file("src/play-jdbc/src/main/scala") ** "*.scala").get ++
          (file("src/play/target/scala-" + sbv + "/src_managed/main/views/html/helper") ** "*.scala").get
      val options = Seq("-sourcepath", base.getAbsolutePath, "-doc-source-url", "https://github.com/playframework/Play20/tree/" + BuildSettings.buildVersion + "/framework€{FILE_PATH}.scala")
      new Scaladoc(10, cs.scalac)("Play " + BuildSettings.buildVersion + " Scala API", sourceFiles, classpath.map(_.data) ++ allJars, file("../documentation/api/scala"), options, s.log)

      // Javadoc
      val javaSources = Seq(
        file("src/play/src/main/java"),
        file("src/play-test/src/main/java"),
        file("src/play-java/src/main/java"),
        file("src/play-java-ebean/src/main/java"),
        file("src/play-java-jdbc/src/main/java"),
        file("src/play-java-jpa/src/main/java")).mkString(":")
      val javaApiTarget = file("../documentation/api/java")
      val javaClasspath = classpath.map(_.data).mkString(":")
      """javadoc -windowtitle playframework -doctitle Play&nbsp;""" + BuildSettings.buildVersion + """&nbsp;Java&nbsp;API  -sourcepath %s -d %s -subpackages play -exclude play.api:play.core -classpath %s""".format(javaSources, javaApiTarget, javaClasspath) ! s.log

  }

  // ----- Build repo

  val buildRepository = TaskKey[Unit]("build-repository")
  val buildRepositoryTask = TaskKey[Unit]("build-repository") <<= (resetRepository, update, update in test, publishLocal, scalaVersion, streams) map {
    (repository, updated, testUpdated, published, scalaVersion, s) =>

      def checksum(algo: String)(bytes: Array[Byte]) = {
        import java.security.MessageDigest
        val digest = MessageDigest.getInstance(algo)
        digest.reset()
        digest.update(bytes)
        digest.digest().map(0xFF & _).map {
          "%02x".format(_)
        }.foldLeft("") {
          _ + _
        }
      }

      def copyWithChecksums(files: (File, File)) {
        IO.copyFile(files._1, files._2)
        Seq("md5", "sha1").foreach {
          algo =>
            IO.write(file(files._2.getAbsolutePath + "." + algo), checksum(algo)(IO.readBytes(files._2)))
        }
      }

      def writeWithChecksums(f: File, content: String) {
        IO.write(f, content)
        Seq("md5", "sha1").foreach {
          algo =>
            IO.write(file(f.getAbsolutePath + "." + algo), checksum(algo)(content.getBytes))
        }
      }

      // Retrieve all ivy files from cache
      // (since we cleaned the cache and run update just before, all these dependencies are useful)
      // Remove SBT plugins (which live in scala_<buildScalaVersionForSbt>)
      val ivyFiles = ((repository / "../cache" * "*").filter {
        d =>
          d.isDirectory && d.getName != "scala_%s".format(buildScalaVersionForSbt)
      } ** "ivy-*.xml").get

      // From the ivy files, deduct the dependencies
      val dependencies = ivyFiles.map {
        descriptor =>
          val organization = descriptor.getParentFile.getParentFile.getName
          val name = descriptor.getParentFile.getName
          val version = descriptor.getName.drop(4).dropRight(4)
          descriptor -> (organization, name, version)
      }

      // Resolve artifacts for these dependencies (only jars)
      val dependenciesWithArtifacts = dependencies.map {
        case (descriptor, (organization, name, version)) => {
          var jars = (descriptor.getParentFile ** ("*-" + version + ".jar")).get
          s.log.info("Found dependency %s::%s::%s -> %s".format(
            organization, name, version, jars.map(_.getName).mkString(", ")))
          (descriptor, jars, (organization, name, version))
        }
      }

      // Build the local repository from these informations
      dependenciesWithArtifacts.foreach {
        case (descriptor, jars, (organization, name, version)) => {
          val dependencyDir = repository / organization / name / version
          val artifacts = jars.map(j => dependencyDir / j.getParentFile.getName / (j.getName.dropRight(5 + version.size) + ".jar"))
          val ivy = dependencyDir / "ivys/ivy.xml"

          (Seq(descriptor -> ivy) ++ jars.zip(artifacts)).foreach(copyWithChecksums)
        }
      }

      // Special sbt plugins
      val pluginIvyFiles = ((repository / "../cache/scala_%s/sbt_%s".format(buildScalaVersionForSbt, buildSbtMajorVersion) * "*").filter {
        d =>
          d.isDirectory && d.getName != "play"
      } ** "ivy-*.xml").get

      // From the ivy files, deduct the dependencies
      val pluginDependencies = pluginIvyFiles.map {
        descriptor =>
          val organization = descriptor.getParentFile.getParentFile.getName
          val name = descriptor.getParentFile.getName
          val version = descriptor.getName.drop(4).dropRight(4)
          descriptor -> (organization, name, version)
      }

      // Resolve artifacts for these dependencies (only jars)
      val pluginDependenciesWithArtifacts = pluginDependencies.map {
        case (descriptor, (organization, name, version)) => {
          var jars = (descriptor.getParentFile ** ("*-" + version + ".jar")).get
          s.log.info("Found plugin dependency %s::%s::%s -> %s".format(
            organization, name, version, jars.map(_.getName).mkString(", ")))
          (descriptor, jars, (organization, name, version))
        }
      }

      // Build the local repository from these informations
      pluginDependenciesWithArtifacts.foreach {
        case (descriptor, jars, (organization, name, version)) => {
          val dependencyDir = repository / organization / name / "scala_%s".format(buildScalaVersionForSbt) / "sbt_%s".format(buildSbtMajorVersion) / version
          val artifacts = jars.map(j => dependencyDir / j.getParentFile.getName / (j.getName.dropRight(5 + version.size) + ".jar"))
          val ivy = dependencyDir / "ivys/ivy.xml"

          (Seq(descriptor -> ivy) ++ jars.zip(artifacts)).foreach(copyWithChecksums)
        }
      }

  }

  // ----- Dist package

  val dist = TaskKey[File]("dist")
  val distTask = dist <<= (buildRepository, publish, generateAPIDocs) map {
    (_, _, _) =>

      import sbt.NameFilter._

      val root = file("..")
      val packageName = "play-" + buildVersion

      val files = {
        (root ** "*") ---
          (root ** "dist") ---
          (root ** "dist" ** "*") ---
          (root ** "*.log") ---
          (root ** "logs") ---
          (root / "repository/cache") ---
          (root / "repository/cache" ** "*") ---
          (root / "framework/sbt/boot") ---
          (root / "framework/sbt/boot" ** "*") ---
          (root ** "project/project") ---
          (root ** "target") ---
          (root ** "target" ** "*") ---
          (root ** ".*") ---
          (root ** ".git" ** "*") ---
          (root ** "*.lock")
      }

      val zipFile = root / "dist" / (packageName + ".zip")

      IO.delete(root / "dist")
      IO.createDirectory(root / "dist")
      IO.zip(files x rebase(root, packageName), zipFile)

      zipFile
  }

  // ----- Compile templates

  val ScalaTemplates = {
    (classpath: Seq[Attributed[File]], templateEngine: File, sourceDirectory: File, generatedDir: File, streams: sbt.std.TaskStreams[sbt.Project.ScopedKey[_]]) =>
      val classloader = new java.net.URLClassLoader(classpath.map(_.data.toURI.toURL).toArray, this.getClass.getClassLoader)
      val compiler = classloader.loadClass("play.templates.ScalaTemplateCompiler")
      val generatedSource = classloader.loadClass("play.templates.GeneratedSource")

      (generatedDir ** "*.template.scala").get.foreach {
        source =>
          val constructor = generatedSource.getDeclaredConstructor(classOf[java.io.File])
          val sync = generatedSource.getDeclaredMethod("sync")
          val generated = constructor.newInstance(source)
          try {
            sync.invoke(generated)
          } catch {
            case e: java.lang.reflect.InvocationTargetException => {
              val t = e.getTargetException
              t.printStackTrace()
              throw t
            }
          }
      }

      (sourceDirectory ** "*.scala.html").get.foreach {
        template =>
          val compile = compiler.getDeclaredMethod("compile", classOf[java.io.File], classOf[java.io.File], classOf[java.io.File], classOf[String], classOf[String], classOf[String])
          try {
            compile.invoke(null, template, sourceDirectory, generatedDir, "play.api.templates.Html", "play.api.templates.HtmlFormat", "import play.api.templates._\nimport play.api.templates.PlayMagic._")
          } catch {
            case e: java.lang.reflect.InvocationTargetException => {
              streams.log.error("Compilation failed for %s".format(template))
              throw e.getTargetException
            }
          }
      }

      (generatedDir ** "*.scala").get.map(_.getAbsoluteFile)
  }

  def scalaTemplateSourceMappings = (excludeFilter in unmanagedSources, unmanagedSourceDirectories in Compile, baseDirectory) map {
    (excludes, sdirs, base) =>
      val scalaTemplateSources = sdirs.descendantsExcept("*.scala.html", excludes)
      ((scalaTemplateSources --- sdirs --- base) pair (relativeTo(sdirs) | relativeTo(base) | flat)) toSeq
  }

}