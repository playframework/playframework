import sbt._
import Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact

object PlayBuild extends Build {

  import Resolvers._
  import Dependencies._
  import BuildSettings._
  import Generators._
  import LocalSBT._
  import Tasks._

  lazy val SbtLinkProject = PlaySharedJavaProject("SBT-link", "sbt-link")
    .settings(libraryDependencies := link)

  lazy val TemplatesProject = PlayRuntimeProject("Templates", "templates")
    .settings(libraryDependencies := templatesDependencies)

  lazy val RoutesCompilerProject = PlaySbtProject("Routes-Compiler", "routes-compiler")
    .settings(libraryDependencies := routersCompilerDependencies)

  lazy val TemplatesCompilerProject = PlaySbtProject("Templates-Compiler", "templates-compiler")
    .settings(
      libraryDependencies := templatesCompilerDependencies,
      unmanagedJars in Compile <+= (baseDirectory) map {
        b => compilerJar(b / "../..")
      }
    )

  lazy val AnormProject = PlayRuntimeProject("Anorm", "anorm")

  lazy val IterateesProject = PlayRuntimeProject("Play-Iteratees", "iteratees")
    .settings(libraryDependencies := iterateesDependencies)

  lazy val FunctionalProject = PlayRuntimeProject("Play-Functional", "play-functional")

  lazy val DataCommonsProject = PlayRuntimeProject("Play-DataCommons", "play-datacommons")

  lazy val JsonProject = PlayRuntimeProject("Play-Json", "play-json")
    .settings(libraryDependencies := jsonDependencies)
    .dependsOn(IterateesProject, FunctionalProject, DataCommonsProject)

  lazy val PlayExceptionsProject = PlaySharedJavaProject("Play-Exceptions", "play-exceptions",
    testBinaryCompatibility = true)

  lazy val PlayProject = PlayRuntimeProject("Play", "play")
    .settings(
      libraryDependencies := runtime,
      sourceGenerators in Compile <+= sourceManaged in Compile map PlayVersion,
      mappings in(Compile, packageSrc) <++= scalaTemplateSourceMappings,
      parallelExecution in Test := false,
      sourceGenerators in Compile <+= (dependencyClasspath in TemplatesCompilerProject in Runtime, packageBin in TemplatesCompilerProject in Compile, scalaSource in Compile, sourceManaged in Compile, streams) map ScalaTemplates
    ).dependsOn(SbtLinkProject, PlayExceptionsProject, TemplatesProject, IterateesProject, JsonProject)

  lazy val PlayJdbcProject = PlayRuntimeProject("Play-JDBC", "play-jdbc")
    .settings(libraryDependencies := jdbcDeps)
    .dependsOn(PlayProject)

  lazy val PlayJavaJdbcProject = PlayRuntimeProject("Play-Java-JDBC", "play-java-jdbc")
    .dependsOn(PlayJdbcProject)

  lazy val PlayEbeanProject = PlayRuntimeProject("Play-Java-Ebean", "play-java-ebean")
    .settings(
      libraryDependencies := ebeanDeps ++ jpaDeps,
      compile in (Compile) <<= (dependencyClasspath in Compile, compile in Compile, classDirectory in Compile) map {
        (deps, analysis, classes) =>

        // Ebean (really hacky sorry)
          val cp = deps.map(_.data.toURL).toArray :+ classes.toURL
          val cl = new java.net.URLClassLoader(cp)

          val t = cl.loadClass("com.avaje.ebean.enhance.agent.Transformer").getConstructor(classOf[Array[URL]], classOf[String]).newInstance(cp, "debug=0").asInstanceOf[AnyRef]
          val ft = cl.loadClass("com.avaje.ebean.enhance.ant.OfflineFileTransform").getConstructor(
            t.getClass, classOf[ClassLoader], classOf[String], classOf[String]
          ).newInstance(t, ClassLoader.getSystemClassLoader, classes.getAbsolutePath, classes.getAbsolutePath).asInstanceOf[AnyRef]

          ft.getClass.getDeclaredMethod("process", classOf[String]).invoke(ft, "play/db/ebean/**")

          analysis
      }
    ).dependsOn(PlayJavaJdbcProject)

  lazy val PlayJpaProject = PlayRuntimeProject("Play-Java-JPA", "play-java-jpa")
    .settings(libraryDependencies := jpaDeps)
    .dependsOn(PlayJavaJdbcProject)

  lazy val PlayJavaProject = PlayRuntimeProject("Play-Java", "play-java")
    .settings(libraryDependencies := javaDeps)
    .dependsOn(PlayProject)

  lazy val PlayTestProject = PlayRuntimeProject("Play-Test", "play-test")
    .settings(
      libraryDependencies := testDependencies,
      parallelExecution in Test := false
    ).dependsOn(PlayProject)

  lazy val SbtPluginProject = PlaySbtProject("SBT-Plugin", "sbt-plugin")
    .settings(
      sbtPlugin := true,
      publishMavenStyle := false,
      libraryDependencies := sbtDependencies,
      libraryDependencies += "com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.1" extra("sbtVersion" -> buildSbtVersionBinaryCompatible, "scalaVersion" -> buildScalaVersionForSbt),
      libraryDependencies += "com.typesafe.sbtidea" % "sbt-idea" % "1.1.1" extra("sbtVersion" -> buildSbtVersionBinaryCompatible, "scalaVersion" -> buildScalaVersionForSbt),
      libraryDependencies += "org.specs2" %% "specs2" % "1.12.3" % "test" exclude("javax.transaction", "jta"),
      unmanagedJars in Compile <++= (baseDirectory) map {
        b => sbtJars(b / "../..")
      },
      publishTo := Some(playIvyRepository)
    ).dependsOn(SbtLinkProject, PlayExceptionsProject, RoutesCompilerProject, TemplatesCompilerProject, ConsoleProject)

  // todo this can be 2.10
  lazy val ConsoleProject = PlaySbtProject("Console", "console")
    .settings(
      libraryDependencies := consoleDependencies,
      sourceGenerators in Compile <+= sourceManaged in Compile map PlayVersion,
      unmanagedJars in Compile <++= (baseDirectory) map {
        b => sbtJars(b / "../..")
      }
    )

  lazy val PlayFiltersHelpersProject = PlayRuntimeProject("Filters-Helpers", "play-filters-helpers")
    .dependsOn(PlayProject)

  val Root = Project(
    "Root",
    file("."))
    .settings(playCommonSettings: _*)
    .settings(
      libraryDependencies := (runtime ++ jdbcDeps),
      cleanFiles ++= Seq(file("../dist"), file("../repository/local")),
      resetRepositoryTask,
      buildRepositoryTask,
      distTask,
      generateAPIDocsTask,
      publish := {}
    ).aggregate(
    PlayProject,
    SbtLinkProject,
    AnormProject,
    TemplatesProject,
    TemplatesCompilerProject,
    IterateesProject,
    FunctionalProject,
    DataCommonsProject,
    JsonProject,
    RoutesCompilerProject,
    PlayProject,
    PlayJdbcProject,
    PlayJavaProject,
    PlayJavaJdbcProject,
    PlayEbeanProject,
    PlayJpaProject,
    SbtPluginProject,
    ConsoleProject,
    PlayTestProject,
    PlayExceptionsProject,
    PlayFiltersHelpersProject
  )

  object BuildSettings {

    val experimental = Option(System.getProperty("experimental")).filter(_ == "true").map(_ => true).getOrElse(false)

    val buildOrganization = "play"
    val buildVersion = Option(System.getProperty("play.version")).filterNot(_.isEmpty).getOrElse("2.0-unknown")
    val buildWithDoc = Option(System.getProperty("generate.doc")).isDefined
    val previousVersion = "2.1.0"
    val previousScalaVersion = "2.10"
    val buildScalaVersion = "2.10.0"
    val buildScalaVersionForSbt = "2.9.2"
    val buildSbtVersion = "0.12.2"
    val buildSbtMajorVersion = "0.12"
    val buildSbtVersionBinaryCompatible = "0.12"

    val playCommonSettings = Seq(
      organization := buildOrganization,
      version := buildVersion,
      scalaVersion := buildScalaVersion,
      scalaBinaryVersion := CrossVersion.binaryScalaVersion(buildScalaVersion),
      ivyLoggingLevel := UpdateLogging.DownloadOnly,
      publishTo := Some(playRepository),
      javacOptions ++= Seq("-source", "1.6", "-target", "1.6", "-encoding", "UTF-8"),
      javacOptions in doc := Seq("-source", "1.6"),
      resolvers += typesafe
    )

    def PlaySharedJavaProject(name: String, dir: String, testBinaryCompatibility: Boolean = false): Project = {
      val bcSettings: Seq[Setting[_]] = if (testBinaryCompatibility) {
        mimaDefaultSettings ++ Seq(previousArtifact := Some("play" % name % previousVersion))
      } else Nil
      Project(name, file("src/" + dir))
        .settings(playCommonSettings: _*)
        .settings(bcSettings: _*)
        .settings(
          autoScalaLibrary := false,
          crossPaths := false,
          publishArtifact in packageDoc := buildWithDoc,
          publishArtifact in(Compile, packageSrc) := true
        )
    }

    def PlayRuntimeProject(name: String, dir: String): Project = {
      Project(name, file("src/" + dir))
        .settings(playCommonSettings: _*)
        .settings(mimaDefaultSettings: _*)
        .settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)
        .settings(playRuntimeSettings(name): _*)
    }

    def playRuntimeSettings(name: String): Seq[Setting[_]] = Seq(
      previousArtifact := Some("play" %% name % previousVersion),
      scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature"),
      publishArtifact in packageDoc := buildWithDoc,
      publishArtifact in(Compile, packageSrc) := true
    )

    def PlaySbtProject(name: String, dir: String): Project = {
      Project(name, file("src/" + dir))
        .settings(playCommonSettings: _*)
        .settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)
        .settings(
          scalaVersion := buildScalaVersionForSbt,
          scalaBinaryVersion := CrossVersion.binaryScalaVersion(buildScalaVersionForSbt),
          publishTo := Some(playRepository),
          publishArtifact in packageDoc := false,
          publishArtifact in(Compile, packageSrc) := false,
          scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked")
      )

    }

  }

  object LocalSBT {

    import BuildSettings._

    def isJar(f: java.io.File) = f.getName.endsWith(".jar")

    def sbtJars(baseDirectory: File): Seq[java.io.File] = {
      (baseDirectory / ("sbt/boot/scala-" + buildScalaVersionForSbt + "/org.scala-sbt/sbt/" + buildSbtVersion)).listFiles.filter(isJar) ++
        (baseDirectory / ("sbt/boot/scala-" + buildScalaVersionForSbt + "/org.scala-sbt/sbt/" + buildSbtVersion + "/xsbti")).listFiles.filter(isJar) ++
        Seq(baseDirectory / ("sbt/boot/scala-" + buildScalaVersionForSbt + "/lib/jline.jar"))
    }

    def compilerJar(baseDirectory: File): java.io.File = {
      baseDirectory / ("sbt/boot/scala-" + buildScalaVersionForSbt + "/lib/scala-compiler.jar")
    }
  }

  object Resolvers {

    import BuildSettings._

    val playLocalRepository = Resolver.file("Play Local Repository", file("../repository/local"))(Resolver.ivyStylePatterns)

    val typesafe = "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

    val typesafeReleases = "Typesafe Releases Repository" at "https://typesafe.artifactoryonline.com/typesafe/maven-releases/"
    val typesafeSnapshot = "Typesafe Snapshots Repository" at "https://typesafe.artifactoryonline.com/typesafe/maven-snapshots/"
    val playRepository = if (buildVersion.endsWith("SNAPSHOT")) typesafeSnapshot else typesafeReleases

    val typesafeIvyReleases = Resolver.url("Typesafe Ivy Releases Repository", url("https://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
    val typesafeIvySnapshot = Resolver.url("Typesafe Ivy Snapshots Repository", url("https://typesafe.artifactoryonline.com/typesafe/ivy-snapshots/"))(Resolver.ivyStylePatterns)

    val playIvyRepository = if (buildVersion.endsWith("SNAPSHOT")) typesafeIvySnapshot else typesafeIvyReleases
  }

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
          """.stripMargin.format(BuildSettings.buildVersion, BuildSettings.buildScalaVersion)
        )
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
          file("src/play-java-jpa/src/main/java")
        ).mkString(":")
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
            descriptor ->(organization, name, version)
        }

        // Resolve artifacts for these dependencies (only jars)
        val dependenciesWithArtifacts = dependencies.map {
          case (descriptor, (organization, name, version)) => {
            var jars = (descriptor.getParentFile ** ("*-" + version + ".jar")).get
            s.log.info("Found dependency %s::%s::%s -> %s".format(
              organization, name, version, jars.map(_.getName).mkString(", ")
            ))
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
            descriptor ->(organization, name, version)
        }

        // Resolve artifacts for these dependencies (only jars)
        val pluginDependenciesWithArtifacts = pluginDependencies.map {
          case (descriptor, (organization, name, version)) => {
            var jars = (descriptor.getParentFile ** ("*-" + version + ".jar")).get
            s.log.info("Found plugin dependency %s::%s::%s -> %s".format(
              organization, name, version, jars.map(_.getName).mkString(", ")
            ))
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

}
