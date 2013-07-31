import sbt._
import Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.{previousArtifact, binaryIssueFilters}
import com.typesafe.tools.mima.core._

object PlayBuild extends Build {

    import Resolvers._
    import Dependencies._
    import BuildSettings._
    import Generators._
    import LocalSBT._
    import Tasks._

    lazy val SbtLinkProject = Project(
        "SBT-link",
        file("src/sbt-link"),
        settings = buildSettings ++ Seq(
            autoScalaLibrary := false,
            libraryDependencies := link,
            publishTo := Some(playRepository),
            javacOptions ++= Seq("-source","1.6","-target","1.6", "-encoding", "UTF-8"),
            javacOptions in doc := Seq("-source", "1.6"),
            publishArtifact in packageDoc := buildWithDoc,
            publishArtifact in (Compile, packageSrc) := true,
            crossPaths := false
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)

    lazy val TemplatesProject = Project(
        "Templates",
        file("src/templates"),
        settings = buildSettingsWithMIMA ++ Seq(
            previousArtifact := Some("play" % {"templates_"+previousScalaVersion} % previousVersion),
            publishTo := Some(playRepository),
            libraryDependencies := templatesDependencies,
            publishArtifact in packageDoc := buildWithDoc,
            publishArtifact in (Compile, packageSrc) := true,
            scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked")
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)

    lazy val RoutesCompilerProject = Project(
        "Routes-Compiler",
        file("src/routes-compiler"),
        settings = buildSettings ++ Seq(
            scalaVersion := buildScalaVersionForSbt,
            scalaBinaryVersion  := CrossVersion.binaryScalaVersion(buildScalaVersionForSbt),
            publishTo := Some(playRepository),
            libraryDependencies := routersCompilerDependencies,
            publishArtifact in packageDoc := false,
            publishArtifact in (Compile, packageSrc) := false,
            scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked")
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)

    lazy val TemplatesCompilerProject = Project(
        "Templates-Compiler",
        file("src/templates-compiler"),
        settings = buildSettings ++ Seq(
            scalaVersion := buildScalaVersionForSbt,
            scalaBinaryVersion  := CrossVersion.binaryScalaVersion(buildScalaVersionForSbt),
            publishTo := Some(playRepository),
            libraryDependencies := templatesCompilerDependencies,
            publishArtifact in packageDoc := false,
            publishArtifact in (Compile, packageSrc) := false,
            unmanagedJars in Compile <+= (baseDirectory) map { b => compilerJar(b / "../..") },
            scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked")
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)

    lazy val AnormProject = Project(
        "Anorm",
        file("src/anorm"),
        settings = buildSettingsWithMIMA ++ Seq(
            previousArtifact := Some("play" % {"anorm_"+previousScalaVersion} % previousVersion),
            publishTo := Some(playRepository),
            scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked", "-feature"),
            publishArtifact in packageDoc := buildWithDoc,
            publishArtifact in (Compile, packageSrc) := true
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)

    lazy val IterateesProject = Project(
        "Play-Iteratees",
        file("src/iteratees"),
        settings = buildSettingsWithMIMA ++ Seq(
            previousArtifact := Some("play" % {"play-iteratees_"+previousScalaVersion} % previousVersion),
            libraryDependencies := iterateesDependencies,
            publishTo := Some(playRepository),
            scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked", "-feature"),
            publishArtifact in packageDoc := buildWithDoc,
            publishArtifact in (Compile, packageSrc) := true
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)

    lazy val PlayExceptionsProject = Project(
        "Play-Exceptions",
        file("src/play-exceptions"),
        settings = buildSettingsWithMIMA ++ Seq(
            autoScalaLibrary := false,
            previousArtifact := Some("play" % "play-exceptions" % previousVersion),
            publishTo := Some(playRepository),
            javacOptions ++= Seq("-source","1.6","-target","1.6", "-encoding", "UTF-8"),
            javacOptions in doc := Seq("-source", "1.6"),
            publishArtifact in packageDoc := buildWithDoc,
            publishArtifact in (Compile, packageSrc) := true,
            crossPaths := false
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)

    lazy val PlayProject = Project(
        "Play",
        file("src/play"),
        settings = buildSettingsWithMIMA ++ Seq(
            previousArtifact := Some("play" % {"play_"+previousScalaVersion} % previousVersion),
            libraryDependencies := runtime,
            sourceGenerators in Compile <+= sourceManaged in Compile map PlayVersion,
            publishTo := Some(playRepository),
            scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked", "-feature"),
            javacOptions ++= Seq("-source","1.6","-target","1.6", "-encoding", "UTF-8"),
            javacOptions in doc := Seq("-source", "1.6"),
            publishArtifact in packageDoc := buildWithDoc,
            publishArtifact in (Compile, packageSrc) := true,
            mappings in (Compile, packageSrc) <++= scalaTemplateSourceMappings,
            parallelExecution in Test := false,
            binaryIssueFilters ++= Seq(
              ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.utils.ProxyDriver.getParentLogger"),
              ProblemFilters.exclude[MissingTypesProblem]("play.core.DynamicPart$"),
              ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.core.DynamicPart.apply"),
              ProblemFilters.exclude[MissingMethodProblem]("play.core.DynamicPart.toString"),
              ProblemFilters.exclude[MissingMethodProblem]("play.core.DynamicPart.copy"),
              ProblemFilters.exclude[MissingMethodProblem]("play.core.j.JavaAction.method"),
              ProblemFilters.exclude[MissingMethodProblem]("play.core.j.JavaAction.parser"),
              ProblemFilters.exclude[MissingMethodProblem]("play.core.j.JavaAction.controller"),
              ProblemFilters.exclude[MissingMethodProblem]("play.core.j.JavaAction.annotations"),
              ProblemFilters.exclude[MissingMethodProblem]("play.core.server.netty.RequestBodyHandler.newRequestBodyHandler"),
              ProblemFilters.exclude[MissingMethodProblem]("play.core.server.netty.RequestBodyHandler.newRequestBodyUpstreamHandler"),
              ProblemFilters.exclude[MissingMethodProblem]("play.core.server.netty.PlayDefaultUpstreamHandler.play$core$server$netty$PlayDefaultUpstreamHandler$$bodyEnumerator$1"),
              ProblemFilters.exclude[MissingMethodProblem]("play.core.server.netty.PlayDefaultUpstreamHandler.newRequestBodyHandler")
            ),
            sourceGenerators in Compile <+= (dependencyClasspath in TemplatesCompilerProject in Runtime, packageBin in TemplatesCompilerProject in Compile, scalaSource in Compile, sourceManaged in Compile, streams) map ScalaTemplates
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)
    .dependsOn(SbtLinkProject, PlayExceptionsProject, TemplatesProject, IterateesProject)

    lazy val PlayJdbcProject = Project(
        "Play-JDBC",
        file("src/play-jdbc"),
        settings = buildSettingsWithMIMA ++ Seq(
            previousArtifact := Some("play" % {"play-jdbc_"+previousScalaVersion} % previousVersion),
            libraryDependencies := jdbcDeps,
            publishTo := Some(playRepository),
            scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked", "-feature"),
            javacOptions ++= Seq("-source","1.6","-target","1.6", "-encoding", "UTF-8"),
            javacOptions in doc := Seq("-source", "1.6"),
            publishArtifact in packageDoc := buildWithDoc,
            publishArtifact in (Compile, packageSrc) := true
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)
    .dependsOn(PlayJavaProject)

    lazy val PlayJavaJdbcProject = Project(
        "Play-Java-JDBC",
        file("src/play-java-jdbc"),
        settings = buildSettingsWithMIMA ++ Seq(
            previousArtifact := Some("play" % {"play-java-jdbc_"+previousScalaVersion} % previousVersion),
            publishTo := Some(playRepository),
            scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked", "-feature"),
            javacOptions ++= Seq("-source","1.6","-target","1.6", "-encoding", "UTF-8"),
            javacOptions in doc := Seq("-source", "1.6"),
            publishArtifact in packageDoc := buildWithDoc,
            publishArtifact in (Compile, packageSrc) := true
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)
    .dependsOn(PlayJdbcProject)

    lazy val PlayEbeanProject = Project(
        "Play-Java-Ebean",
        file("src/play-java-ebean"),
        settings = buildSettingsWithMIMA ++ Seq(
            previousArtifact := Some("play" % {"play-java-ebean_"+previousScalaVersion} % previousVersion),
            libraryDependencies := ebeanDeps ++ jpaDeps,
            publishTo := Some(playRepository),
            scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked", "-feature"),
            javacOptions ++= Seq("-source","1.6","-target","1.6", "-encoding", "UTF-8"),
            javacOptions in doc := Seq("-source", "1.6"),
            publishArtifact in packageDoc := buildWithDoc,
            publishArtifact in (Compile, packageSrc) := true,
            compile in (Compile) <<= (dependencyClasspath in Compile, compile in Compile, classDirectory in Compile) map { (deps,analysis,classes) =>

                // Ebean (really hacky sorry)

                val cp = deps.map(_.data.toURL).toArray :+ classes.toURL
                val cl = new java.net.URLClassLoader(cp)

                val t = cl.loadClass("com.avaje.ebean.enhance.agent.Transformer").getConstructor(classOf[Array[URL]], classOf[String]).newInstance(cp, "debug=0").asInstanceOf[AnyRef]
                val ft = cl.loadClass("com.avaje.ebean.enhance.ant.OfflineFileTransform").getConstructor(
                    t.getClass, classOf[ClassLoader], classOf[String], classOf[String]
                ).newInstance(t, ClassLoader.getSystemClassLoader, classes.getAbsolutePath, classes.getAbsolutePath).asInstanceOf[AnyRef]

                ft.getClass.getDeclaredMethod("process", classOf[String]).invoke(ft,"play/db/ebean/**")

                analysis
            }
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)
    .dependsOn(PlayJavaJdbcProject)

    lazy val PlayJpaProject = Project(
        "Play-Java-JPA",
        file("src/play-java-jpa"),
        settings = buildSettingsWithMIMA ++ Seq(
            previousArtifact := Some("play" % {"play-java-jpa_"+previousScalaVersion} % previousVersion),
            libraryDependencies := jpaDeps,
            publishTo := Some(playRepository),
            scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked"),
            javacOptions ++= Seq("-source","1.6","-target","1.6", "-encoding", "UTF-8"),
            javacOptions in doc := Seq("-source", "1.6"),
            publishArtifact in packageDoc := buildWithDoc,
            publishArtifact in (Compile, packageSrc) := true
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)
    .dependsOn(PlayJavaJdbcProject)

    lazy val PlayJavaProject = Project(
        "Play-Java",
        file("src/play-java"),
        settings = buildSettingsWithMIMA ++ Seq(
            previousArtifact := Some("play" % {"play-java_"+previousScalaVersion} % previousVersion),
            libraryDependencies := javaDeps,
            publishTo := Some(playRepository),
            scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked"),
            javacOptions ++= Seq("-source","1.6","-target","1.6", "-encoding", "UTF-8"),
            javacOptions in doc := Seq("-source", "1.6"),
            publishArtifact in packageDoc := buildWithDoc,
            publishArtifact in (Compile, packageSrc) := true
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)
    .dependsOn(PlayProject)

    lazy val PlayTestProject = Project(
        "Play-Test",
        file("src/play-test"),
        settings = buildSettingsWithMIMA ++ Seq(
            previousArtifact := Some("play" % {"play-test_"+previousScalaVersion} % previousVersion),
            libraryDependencies := testDependencies,
            publishTo := Some(playRepository),
            scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked", "-feature"),
            javacOptions ++= Seq("-source","1.6","-target","1.6", "-encoding", "UTF-8"),
            javacOptions in doc := Seq("-source", "1.6"),
            publishArtifact in packageDoc := buildWithDoc,
            publishArtifact in (Compile, packageSrc) := true,
            parallelExecution in Test := false
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*).dependsOn(PlayProject)

    // This project is just for testing Play, not really a public artifact
    lazy val PlayIntegrationTestProject = Project(
      "Play-Integration-Test",
      file("src/play-integration-test"),
      settings = buildSettingsWithMIMA ++ Seq(
        libraryDependencies := runtime,
        publishTo := Some(playRepository),
        scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked", "-feature"),
        javacOptions ++= Seq("-source","1.6","-target","1.6", "-encoding", "UTF-8"),
        javacOptions in doc := Seq("-source", "1.6"),
        parallelExecution in Test := false
      )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)
      .dependsOn(PlayProject, PlayTestProject)

    lazy val SbtPluginProject = Project(
        "SBT-Plugin",
        file("src/sbt-plugin"),
        settings = buildSettings ++ Seq(
            scalaVersion := buildScalaVersionForSbt,
            scalaBinaryVersion  := CrossVersion.binaryScalaVersion(buildScalaVersionForSbt),
            sbtPlugin := true,
            publishMavenStyle := false,
            libraryDependencies := sbtDependencies,
            libraryDependencies += "com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.1" extra("sbtVersion" -> buildSbtVersionBinaryCompatible, "scalaVersion" -> buildScalaVersionForSbt),
            libraryDependencies += "com.typesafe.sbtidea" % "sbt-idea" % "1.1.1" extra("sbtVersion" -> buildSbtVersionBinaryCompatible, "scalaVersion" -> buildScalaVersionForSbt),
            unmanagedJars in Compile <++= (baseDirectory) map { b => sbtJars(b / "../..") },
            publishTo := Some(playIvyRepository),
            scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked"),
            publishArtifact in packageDoc := buildWithDoc,
            publishArtifact in (Compile, packageSrc) := true
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)
    .dependsOn(SbtLinkProject, PlayExceptionsProject, RoutesCompilerProject, TemplatesCompilerProject, ConsoleProject)


    lazy val ConsoleProject = Project(
        "Console",
        file("src/console"),
        settings = buildSettings ++ Seq(
            scalaVersion := buildScalaVersionForSbt,
            scalaBinaryVersion  := CrossVersion.binaryScalaVersion(buildScalaVersionForSbt),
            libraryDependencies := consoleDependencies,
            sourceGenerators in Compile <+= sourceManaged in Compile map PlayVersion,
            unmanagedJars in Compile <++=  (baseDirectory) map { b => sbtJars(b / "../..") },
            publishTo := Some(playRepository),
            scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked"),
            publishArtifact in packageDoc := buildWithDoc,
            publishArtifact in (Compile, packageSrc) := true
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)

    lazy val PlayFiltersHelpersProject = Project(
        "Filters-Helpers",
        file("src/play-filters-helpers"),
        settings = buildSettingsWithMIMA ++ Seq(
            previousArtifact := Some("play" % {"filters-helpers_"+previousScalaVersion} % previousVersion),
            libraryDependencies := runtime,
            publishTo := Some(playRepository),
            scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked", "-feature"),
            javacOptions ++= Seq("-source","1.6","-target","1.6", "-encoding", "UTF-8"),
            javacOptions in doc := Seq("-source", "1.6"),
            publishArtifact in packageDoc := buildWithDoc,
            publishArtifact in (Compile, packageSrc) := true
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)
    .dependsOn(PlayProject)

    val Root = Project(
        "Root",
        file("."),
        settings = buildSettings ++ Seq(
            libraryDependencies := (runtime ++ jdbcDeps),
            cleanFiles ++= Seq(file("../dist"), file("../repository/local")),
            resetRepositoryTask,
            buildRepositoryTask,
            distTask,
            generateAPIDocsTask,
            publish := {}
        )
    ).settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)
     .aggregate(
        PlayProject,
        SbtLinkProject,
        AnormProject,
        TemplatesProject,
        TemplatesCompilerProject,
        IterateesProject,
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
        PlayFiltersHelpersProject,
        PlayIntegrationTestProject
    )

    object BuildSettings {

        val experimental = Option(System.getProperty("experimental")).filter(_ == "true").map(_ => true).getOrElse(false)

        val buildOrganization = "play"
        val buildVersion      = Option(System.getProperty("play.version")).filterNot(_.isEmpty).getOrElse("2.0-unknown")
        val buildWithDoc      = Option(System.getProperty("generate.doc")).isDefined
        val previousVersion   = "2.1.0"
        val previousScalaVersion = "2.10"
        val buildScalaVersion = "2.10.0"
        val buildScalaVersionForSbt = "2.9.2"
        val buildSbtVersion   = "0.12.2"
        val buildSbtMajorVersion = "0.12"
        val buildSbtVersionBinaryCompatible = "0.12"

        val buildSettings = Defaults.defaultSettings ++ Seq (
            resolvers           += typesafe,
            organization        := buildOrganization,
            version             := buildVersion,
            scalaVersion        := buildScalaVersion,
            scalaBinaryVersion  := CrossVersion.binaryScalaVersion(buildScalaVersion),
            ivyLoggingLevel     := UpdateLogging.DownloadOnly
        )
        val buildSettingsWithMIMA = buildSettings ++ mimaDefaultSettings
    }

    object LocalSBT {

        import BuildSettings._
        def isJar(f:java.io.File) = f.getName.endsWith(".jar")

        def sbtJars(baseDirectory: File): Seq[java.io.File] = {
            (baseDirectory / ("sbt/boot/scala-" + buildScalaVersionForSbt + "/org.scala-sbt/sbt/" + buildSbtVersion)).listFiles.filter(isJar) ++
            (baseDirectory / ("sbt/boot/scala-" + buildScalaVersionForSbt + "/org.scala-sbt/sbt/" + buildSbtVersion + "/xsbti")).listFiles.filter(isJar) ++
            Seq(baseDirectory / ("sbt/boot/scala-" + buildScalaVersionForSbt + "/lib/jline.jar"))
        }

        def compilerJar(baseDirectory: File):java.io.File = {
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

    object Dependencies {

      // Some common dependencies here so they don't need to be declared over and over
      val specsBuild = "org.specs2" %% "specs2" % "1.13"
      val scalaIoFileBuild = "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2"


      val jdbcDeps = Seq(
            ("com.jolbox"                       %    "bonecp"                   %   "0.7.1.RELEASE" notTransitive())
              .exclude("com.google.guava", "guava")
              .exclude("org.slf4j", "slf4j-api"),

            "com.h2database"                    %    "h2"                       %   "1.3.168",

            "tyrex"                             %    "tyrex"                    %   "1.0.1",

            specsBuild                          %   "test"
        )

        val ebeanDeps = Seq(
            "org.avaje.ebeanorm"                %    "avaje-ebeanorm"           %   "3.1.2" exclude("javax.persistence", "persistence-api")
        )

        val jpaDeps = Seq(
            "org.hibernate.javax.persistence"   %    "hibernate-jpa-2.0-api"    %   "1.0.1.Final"
        )

        val javaDeps = Seq(

            "org.yaml"                          %    "snakeyaml"                %   "1.10",
            "org.hibernate"                     %    "hibernate-validator"      %   "4.3.0.Final",

            ("org.springframework"              %    "spring-context"           %   "3.1.2.RELEASE" notTransitive())
              .exclude("org.springframework", "spring-aop")
              .exclude("org.springframework", "spring-beans")
              .exclude("org.springframework", "spring-core")
              .exclude("org.springframework", "spring-expression")
              .exclude("org.springframework", "spring-asm")
            ,

            ("org.springframework"              %    "spring-core"              %   "3.1.2.RELEASE" notTransitive())
              .exclude("org.springframework", "spring-asm")
              .exclude("commons-logging", "commons-logging")
            ,

            ("org.springframework"              %    "spring-beans"             %   "3.1.2.RELEASE" notTransitive())
              .exclude("org.springframework", "spring-core")
            ,

            "org.javassist"                     %    "javassist"                %   "3.16.1-GA",

            ("org.reflections"                  %    "reflections"              %   "0.9.8" notTransitive())
              .exclude("com.google.guava", "guava")
              .exclude("javassist", "javassist")
            ,

            "com.google.guava"                  %    "guava"                    %   "13.0.1",

            "com.google.code.findbugs"          %    "jsr305"                   %   "2.0.1",


            "javax.servlet"                     %    "javax.servlet-api"        %   "3.0.1",

            specsBuild %  "test"
        )

        val runtime = Seq(
            "io.netty"                          %    "netty"                    %   "3.6.3.Final",

            "com.typesafe.netty"                %    "netty-http-pipelining"    %   "1.1.1",

            "org.slf4j"                         %    "slf4j-api"                %   "1.6.6",
            "org.slf4j"                         %    "jul-to-slf4j"             %   "1.6.6",
            "org.slf4j"                         %    "jcl-over-slf4j"           %   "1.6.6",

            "ch.qos.logback"                    %    "logback-core"             %   "1.0.7",
            "ch.qos.logback"                    %    "logback-classic"          %   "1.0.7",

            scalaIoFileBuild,

            "com.typesafe.akka"                 %%    "akka-actor"              %   "2.1.0",
            "com.typesafe.akka"                 %%    "akka-slf4j"              %   "2.1.0",

            "org.scala-stm"                     %    "scala-stm_2.10.0"         %   "0.6",

            "joda-time"                         %    "joda-time"                %   "2.1",
            "org.joda"                          %    "joda-convert"             %   "1.2",

            "org.apache.commons"                %    "commons-lang3"            %   "3.1",

            ("com.ning"                         %    "async-http-client"        %   "1.7.6" notTransitive())
              .exclude("org.jboss.netty", "netty")
            ,

            "oauth.signpost"                    %    "signpost-core"            %   "1.2.1.2",
            "oauth.signpost"                    %    "signpost-commonshttp4"    %   "1.2.1.2",

            "org.codehaus.jackson"              %    "jackson-core-asl"         %   "1.9.10",
            "org.codehaus.jackson"              %    "jackson-mapper-asl"       %   "1.9.10",

            "net.sf.ehcache"                    %    "ehcache-core"             %   "2.6.0",

            "javax.transaction"                 %    "jta"                      %   "1.1",

            specsBuild % "test",

            "org.mockito"                       %    "mockito-all"              %   "1.9.0"    %  "test",
            "com.novocode"                      %    "junit-interface"          %   "0.10"     %  "test",

            "org.fluentlenium"                  %    "fluentlenium-festassert"  %   "0.7.3"    %  "test" exclude("org.jboss.netty", "netty"),
            "org.scala-lang"                    %    "scala-reflect"            %   "2.10.0"
        )

        val link = Seq(
            "org.javassist"                     %    "javassist"                %   "3.16.1-GA"
        )


        val routersCompilerDependencies = Seq(
            "com.github.scala-incubator.io"     %%   "scala-io-file"            %   "0.4.1" exclude("javax.transaction", "jta"),
            "org.specs2"                        %%   "specs2"                   %   "1.12.3"    %   "test" exclude("javax.transaction", "jta")
      )

        val templatesCompilerDependencies = Seq(
            "com.github.scala-incubator.io"     %%   "scala-io-file"            %   "0.4.1" exclude("javax.transaction", "jta"),
            "org.specs2"                        %%   "specs2"                   %   "1.12.3"    %   "test"
              exclude("javax.transaction", "jta")
        )


        val sbtDependencies = Seq(
            "com.typesafe"                      %    "config"                   %   "1.0.0",
            "rhino"                             %    "js"                       %   "1.7R2",

            ("com.google.javascript"            %    "closure-compiler"         %   "rr2079.1" notTransitive())
              .exclude("args4j", "args4j")
              .exclude("com.google.guava", "guava")
              .exclude("org.json", "json")
              .exclude("com.google.protobuf", "protobuf-java")
              .exclude("org.apache.ant", "ant")
              .exclude("com.google.code.findbugs", "jsr305")
              .exclude("com.googlecode.jarjar", "jarjar")
              .exclude("junit", "junit")
            ,

            ("com.google.guava"                 %    "guava"                    %   "10.0.1" notTransitive())
              .exclude("com.google.code.findbugs", "jsr305")
            ,

            "com.github.scala-incubator.io"     %%   "scala-io-file"            %   "0.4.1" exclude("javax.transaction", "jta"),

            "org.avaje.ebeanorm"                %    "avaje-ebeanorm"           %   "3.1.2" exclude("javax.persistence", "persistence-api"),

            "com.h2database"                    %    "h2"                       %   "1.3.168",
            "org.javassist"                     %    "javassist"                %   "3.16.1-GA",
            "org.pegdown"                       %    "pegdown"                  %   "1.1.0",

            "net.contentobjects.jnotify"        %    "jnotify"                  %   "0.94"
        )

        val consoleDependencies = Seq(
            "net.databinder.giter8"             %   "giter8_2.9.1"              %   "0.5.0"
        )

        val templatesDependencies = Seq(
            scalaIoFileBuild,
            specsBuild % "test"
        )

        val iterateesDependencies = Seq(
            "org.scala-stm"                     %    "scala-stm_2.10.0"         %   "0.6",
            "com.typesafe"                      %    "config"                   %   "1.0.0",
            scalaIoFileBuild,
            specsBuild % "test"
      )


        val testDependencies = Seq(
            "junit"                             %    "junit-dep"                %   "4.10",
            specsBuild,
            "com.novocode"                      %    "junit-interface"          %   "0.10",

            "org.fluentlenium"                  %    "fluentlenium-festassert"  %   "0.7.3" exclude("org.jboss.netty", "netty")
        )

    }

    object Generators {

        val PlayVersion = { dir:File =>
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
        val generateAPIDocsTask = TaskKey[Unit]("api-docs") <<= (dependencyClasspath in Test, compilers, streams, baseDirectory, scalaBinaryVersion) map { (classpath, cs, s, base, sbv) =>

          val branch = if (BuildSettings.buildVersion.endsWith("-SNAPSHOT")) {
            System.getProperty("git.branch", "master")
          } else {
            BuildSettings.buildVersion
          }

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
          val options = Seq("-sourcepath", base.getAbsolutePath, "-doc-source-url", "https://github.com/playframework/Play20/tree/" + branch + "/framework€{FILE_PATH}.scala")
          new Scaladoc(10, cs.scalac)("Play " + BuildSettings.buildVersion + " Scala API", sourceFiles, classpath.map(_.data) ++ allJars,
          file("../documentation/api/scala"), options , s.log)

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
          """javadoc -notimestamp -windowtitle playframework -doctitle Play&nbsp;""" + BuildSettings.buildVersion + """&nbsp;Java&nbsp;API  -sourcepath %s -d %s -subpackages play -exclude play.api:play.core -classpath %s""".format(javaSources, javaApiTarget, javaClasspath) ! s.log

        }

        // ----- Build repo

        val buildRepository = TaskKey[Unit]("build-repository")
        val buildRepositoryTask = TaskKey[Unit]("build-repository") <<= (resetRepository, update, update in test, publishLocal, scalaVersion, streams) map { (repository, updated, testUpdated, published, scalaVersion, s) =>

            def checksum(algo:String)(bytes:Array[Byte]) = {
                import java.security.MessageDigest
                val digest = MessageDigest.getInstance(algo)
                digest.reset()
                digest.update(bytes)
                digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft(""){_ + _}
            }

            def copyWithChecksums(files:(File,File)) {
                IO.copyFile(files._1, files._2)
                Seq("md5","sha1").foreach { algo =>
                    IO.write(file(files._2.getAbsolutePath + "." + algo), checksum(algo)(IO.readBytes(files._2)))
                }
            }

            def writeWithChecksums(f:File, content:String) {
                IO.write(f, content)
                Seq("md5","sha1").foreach { algo =>
                    IO.write(file(f.getAbsolutePath + "." + algo), checksum(algo)(content.getBytes))
                }
            }

            // Retrieve all ivy files from cache
            // (since we cleaned the cache and run update just before, all these dependencies are useful)
            // Remove SBT plugins (which live in scala_<buildScalaVersionForSbt>)
            val ivyFiles = ((repository / "../cache" * "*").filter { d =>
              d.isDirectory && d.getName != "scala_%s".format(buildScalaVersionForSbt)
            } ** "ivy-*.xml").get

            // From the ivy files, deduct the dependencies
            val dependencies = ivyFiles.map { descriptor =>
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
            val pluginIvyFiles = ((repository / "../cache/scala_%s/sbt_%s".format(buildScalaVersionForSbt, buildSbtMajorVersion) * "*").filter { d =>
              d.isDirectory && d.getName != "play"
            } ** "ivy-*.xml").get

            // From the ivy files, deduct the dependencies
            val pluginDependencies = pluginIvyFiles.map { descriptor =>
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
        val distTask = dist <<= (buildRepository, publish, generateAPIDocs) map { (_,_,_) =>

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

        val ScalaTemplates = { (classpath:Seq[Attributed[File]], templateEngine:File, sourceDirectory:File, generatedDir:File, streams:sbt.std.TaskStreams[sbt.Project.ScopedKey[_]]) =>
            val classloader = new java.net.URLClassLoader(classpath.map(_.data.toURI.toURL).toArray, this.getClass.getClassLoader)
            val compiler = classloader.loadClass("play.templates.ScalaTemplateCompiler")
            val generatedSource = classloader.loadClass("play.templates.GeneratedSource")

            (generatedDir ** "*.template.scala").get.foreach { source =>
                val constructor = generatedSource.getDeclaredConstructor(classOf[java.io.File])
                val sync = generatedSource.getDeclaredMethod("sync")
                val generated = constructor.newInstance(source)
                try {
                    sync.invoke(generated)
                } catch {
                    case e:java.lang.reflect.InvocationTargetException =>{
                        val t = e.getTargetException
                        t.printStackTrace()
                        throw t
                    }
                }
            }

            (sourceDirectory ** "*.scala.html").get.foreach { template =>
                val compile = compiler.getDeclaredMethod("compile", classOf[java.io.File], classOf[java.io.File], classOf[java.io.File], classOf[String], classOf[String], classOf[String])
                try {
                    compile.invoke(null, template, sourceDirectory, generatedDir, "play.api.templates.Html", "play.api.templates.HtmlFormat", "import play.api.templates._\nimport play.api.templates.PlayMagic._")
                } catch {
                    case e:java.lang.reflect.InvocationTargetException => {
                        streams.log.error("Compilation failed for %s".format(template))
                        throw e.getTargetException
                    }
                }
            }

            (generatedDir ** "*.scala").get.map(_.getAbsoluteFile)
        }

        def scalaTemplateSourceMappings = (excludeFilter in unmanagedSources, unmanagedSourceDirectories in Compile, baseDirectory) map { (excludes, sdirs, base) =>
          val scalaTemplateSources = sdirs.descendantsExcept("*.scala.html", excludes)
          ( (scalaTemplateSources --- sdirs --- base) pair (relativeTo(sdirs)|relativeTo(base)|flat)) toSeq
        }


    }

}
