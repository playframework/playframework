/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import java.net.URLClassLoader
import java.util.Locale
import java.util.Optional

import sbt._
import sbt.internal.inc.AnalyzingCompiler
import sbt.internal.inc.LoggedReporter
import sbt.internal.inc.PlainVirtualFile
import sbt.internal.inc.PlainVirtualFileConverter
import sbt.internal.BuildStructure
import sbt.io.Path._
import sbt.util.CacheStoreFactory
import sbt.Keys._

import org.webjars.WebJarExtractor
import xsbti.compile._
import Playdoc.autoImport.playdocPackage

object Docs {
  val Webjars = config("webjars").hide

  val apiDocsInclude        = settingKey[Boolean]("Whether this sub project should be included in the API docs")
  val apiDocsIncludeManaged =
    settingKey[Boolean]("Whether managed sources from this project should be included in the API docs")
  val apiDocsScalaSources = taskKey[Seq[File]]("All the scala sources for all projects")
  val apiDocsJavaSources  = taskKey[Seq[File]]("All the Java sources for all projects")
  val apiDocsClasspath    = taskKey[Seq[File]]("The classpath for API docs generation")
  val apiDocs             = taskKey[File]("Generate the API docs")
  val extractWebjars      = taskKey[File]("Extract webjar contents")
  val allConfs            = taskKey[Seq[(String, File)]]("Gather all configuration files")

  lazy val settings = Seq(
    apiDocsInclude        := false,
    apiDocsIncludeManaged := false,
    apiDocsScalaSources   := Def.taskDyn {
      val pr = thisProjectRef.value
      val bs = buildStructure.value
      Def.task(allSources(Compile, ".scala", pr, bs).value)
    }.value,
    apiDocsClasspath := Def.taskDyn {
      val pr = thisProjectRef.value
      val bs = buildStructure.value
      Def.task(allClasspaths(pr, bs).value)
    }.value,
    apiDocsJavaSources := Def.taskDyn {
      val pr = thisProjectRef.value
      val bs = buildStructure.value
      Def.task(allSources(Compile, ".java", pr, bs).value)
    }.value,
    (Global / allConfs) := Def.taskDyn {
      val pr = thisProjectRef.value
      val bs = buildStructure.value
      Def.task(allConfsTask(pr, bs).value)
    }.value,
    apiDocs := apiDocsTask.value,
    ivyConfigurations += Webjars,
    extractWebjars := extractWebjarContents.value,
    (Compile / packageBin / mappings) ++= {
      val apiBase = apiDocs.value
      val webjars = extractWebjars.value
      // Include documentation and API docs in main binary JAR
      val docBase     = baseDirectory.value / "../../documentation"
      val raw         = (docBase \ "manual" ** "*") +++ (docBase \ "style" ** "*")
      val filtered    = raw.filter(_.getName != ".DS_Store")
      val docMappings = filtered.get().pair(rebase(docBase, "play/docs/content/"))

      val apiDocMappings = (apiBase ** "*").get().pair(rebase(apiBase, "play/docs/content/api"))

      // The play version is added so that resource paths are versioned
      val webjarMappings = webjars.allPaths.pair(rebase(webjars, "play/docs/content/webjars/" + version.value))

      // Gather all the conf files into the project
      val referenceConfMappings = allConfs.value.map {
        case (projectName, conf) => conf -> s"play/docs/content/confs/$projectName/${conf.getName}"
      }

      docMappings ++ apiDocMappings ++ webjarMappings ++ referenceConfMappings
    }
  )

  def playdocSettings: Seq[Setting[?]] = Def.settings(
    Playdoc.projectSettings,
    ivyConfigurations += Webjars,
    extractWebjars := extractWebjarContents.value,
    libraryDependencies ++= Dependencies.playdocWebjarDependencies,
    (playdocPackage / mappings) := {
      val base        = (ThisBuild / baseDirectory).value
      val docBase     = base / "documentation"
      val raw         = (docBase / "manual").allPaths +++ (docBase / "style").allPaths
      val filtered    = raw.filter(_.getName != ".DS_Store")
      val docMappings = filtered.get().pair(relativeTo(docBase))

      // The play version is added so that resource paths are versioned
      val webjars        = extractWebjars.value
      val playVersion    = version.value
      val webjarMappings = webjars.allPaths.pair(rebase(webjars, "webjars/" + playVersion))

      // Gather all the conf files into the project
      val referenceConfs = allConfs.value.map {
        case (projectName, conf) => conf -> s"confs/$projectName/${conf.getName}"
      }

      docMappings ++ webjarMappings ++ referenceConfs
    }
  )

  // This is a specialized task that does not replace "sbt doc" but packages all the doc at once.
  def apiDocsTask = Def.taskDyn {
    val apiDocsDir = Docs.apiDocsDir.value
    if ((packageDoc / publishArtifact).value) {
      genApiScaladocs.value
      genApiJavadocs.value
    }
    fixJavadocLinks(apiDocsDir)
    Def.task(apiDocsDir)
  }

  val apiDocsDir                 = Def.setting(crossTarget.value / "apidocs")
  def apiDocsCache(name: String) = Def.setting(CacheStoreFactory(crossTarget.value / name))

  def genApiScaladocs = Def.task {
    val version = Keys.version.value
    val label   = s"Play $version"

    val commitish   = if (version.endsWith("-SNAPSHOT")) BuildSettings.snapshotBranch else version
    val externalDoc =
      Opts.doc.externalAPI(apiMappings.value).head.replace("-doc-external-doc:", "") // from the "doc" task

    val options = Seq(
      // Note, this is used by the doc-source-url feature to determine the relative path of a given source file.
      // If it's not a prefix of a the absolute path of the source file, the absolute path of that file will be put
      // into the FILE_SOURCE variable below, which is definitely not what we want.
      // Hence it needs to be the base directory for the build, not the base directory for the play-docs project.
      "-sourcepath",
      (ThisBuild / baseDirectory).value.getAbsolutePath,
      "-doc-source-url",
      s"https://github.com/playframework/playframework/tree/${commitish}€{FILE_PATH}.scala",
      s"-doc-external-doc:$externalDoc",
      "-Xsource:3"
    )

    val cache  = apiDocsCache("scalaapidocs.cache").value
    val scalac = compilers.value.scalac().asInstanceOf[AnalyzingCompiler]

    val scaladoc = Doc.scaladoc(label, cache, scalac)

    val sources   = apiDocsScalaSources.value
    val classpath = apiDocsClasspath.value.toList
    val outputDir = apiDocsDir.value / "scala"
    val log       = streams.value.log

    scaladoc(sources, classpath, outputDir, options, 10, log)
  }

  def genApiJavadocs = Def.task {
    val label = s"Play ${version.value}"

    val options = List(
      "-windowtitle",
      label,
      // Adding a user agent when we run `javadoc` was necessary to create link docs
      // with Akka (at least, maybe play too) because doc.akka.io is served by Cloudflare
      // which blocks requests without a User-Agent header.
      // Not sure we need that for Pekko however.
      "-J-Dhttp.agent=Play-Unidoc-Javadoc",
      "-link",
      "https://docs.oracle.com/en/java/javase/17/docs/api/",
      "-link",
      "https://pekko.apache.org/japi/pekko/1.0/",
      "-link",
      "https://pekko.apache.org/japi/pekko-http/1.0/",
      "-notimestamp",
      "-Xmaxwarns",
      "1000",
      "-exclude",
      "play.api:play.core",
      "-source",
      "17"
    )

    val cache     = apiDocsCache("javaapidocs.cache").value
    val javaTools = compilers.value.javaTools

    val javadoc = sbt.inc.Doc.cachedJavadoc(label, cache, javaTools)

    val sources    = apiDocsJavaSources.value.toList
    val classpath  = apiDocsClasspath.value.toList
    val outputDir  = apiDocsDir.value / "java"
    val incToolOpt = IncToolOptions.create(Optional.empty[ClassFileManager](), false)
    val log        = streams.value.log
    val reporter   = new LoggedReporter(10, log)

    javadoc.run(
      sources = sources.map(s => PlainVirtualFile(s.toPath)),
      classpath = classpath.map(s => PlainVirtualFile(s.toPath)),
      converter = PlainVirtualFileConverter.converter,
      outputDirectory = outputDir.toPath,
      options = options,
      incToolOptions = incToolOpt,
      log = log,
      reporter = reporter
    )
  }

  def fixJavadocLinks(apiTarget: File) = {
    val externalJavadocLinks = {
      // Known Java libraries in non-standard locations...
      // All the external Javadoc URLs that must be fixed.
      val nonStandardJavadocLinks = Set(javaApiUrl, jakartaInjectUrl, ehCacheUrl, guiceUrl)

      import Dependencies._
      val standardJavadocModuleIDs = Set(playJson) ++ slf4j

      nonStandardJavadocLinks ++ standardJavadocModuleIDs.map(moduleIDToJavadoc)
    }

    import scala.util.matching.Regex
    import scala.util.matching.Regex.Match

    def javadocLinkRegex(javadocURL: String): Regex = ("""\"(\Q""" + javadocURL + """\E)#([^"]*)\"""").r

    def hasJavadocLink(f: File): Boolean =
      externalJavadocLinks.exists { (javadocURL: String) =>
        javadocLinkRegex(javadocURL).findFirstIn(IO.read(f)).nonEmpty
      }

    val fixJavaLinks: Match => String = m => m.group(1) + "?" + m.group(2).replace(".", "/") + ".html"

    // Maps to Javadoc references in Scaladoc, and fixes the link so that it uses query parameters in
    // Javadoc style to link directly to the referenced class.
    // https://stackoverflow.com/questions/16934488/how-to-link-classes-from-jdk-into-scaladoc-generated-doc/
    (apiTarget ** "*.html").get().filter(hasJavadocLink).foreach { f =>
      val newContent: String = externalJavadocLinks.foldLeft(IO.read(f)) {
        case (oldContent: String, javadocURL: String) =>
          javadocLinkRegex(javadocURL).replaceAllIn(oldContent, fixJavaLinks)
      }
      IO.write(f, newContent)
    }
  }

  // Converts an artifact into a Javadoc URL.
  def artifactToJavadoc(organization: String, name: String, apiVersion: String, jarBaseFile: String): String = {
    val slashedOrg = organization.replace('.', '/')
    if (apiVersion.toLowerCase(Locale.ENGLISH).endsWith("-snapshot")) {
      raw"""https://central.sonatype.com/repository/maven-snapshots/$slashedOrg/$name/$apiVersion/$jarBaseFile-javadoc.jar/!/index.html"""
    } else {
      raw"""https://repo1.maven.org/maven2/$slashedOrg/$name/$apiVersion/$jarBaseFile-javadoc.jar/!/index.html"""
    }
  }

  // Converts an sbt module into a Javadoc URL.
  def moduleIDToJavadoc(id: ModuleID): String = {
    artifactToJavadoc(id.organization, id.name, id.revision, s"${id.name}-${id.revision}")
  }

  val javaApiUrl       = "https://docs.oracle.com/en/java/javase/17/docs/api/index.html"
  val jakartaInjectUrl = "https://jakarta.ee/specifications/dependency-injection/2.0/apidocs/index.html"
  // ehcache has 2.6.11 as version, but latest is only 2.6.9 on the site!
  val ehCacheUrl = raw"https://www.ehcache.org/apidocs/2.6.9/index.html"
  // nonstandard guice location
  val guiceUrl = raw"https://google.github.io/guice/api-docs/${Dependencies.guiceVersion}/javadoc/index.html"

  def allConfsTask(projectRef: ProjectRef, structure: BuildStructure): Task[Seq[(String, File)]] = {
    val projects                = allApiProjects(projectRef.build, structure)
    val unmanagedResourcesTasks = projects.map { ref =>
      def taskFromProject[T](task: TaskKey[T]) = (ref / Compile / task).get(structure.data)

      val projectId = (ref / moduleName).get(structure.data)

      val confs = (ref / Compile / unmanagedResources)
        .get(structure.data)
        .map(_.map { resources =>
          (for {
            conf <- resources.filter(resource =>
              resource.name == "reference.conf" || resource.name.endsWith(".xml") || resource.name
                .endsWith(".default")
            )
            id <- projectId.toSeq
          } yield id -> conf).distinct
        })

      // Join them
      val tasks = confs.toSeq
      tasks.join.map(_.flatten)
    }
    unmanagedResourcesTasks.join.map(_.flatten)
  }

  def allSources(
      conf: Configuration,
      extension: String,
      projectRef: ProjectRef,
      structure: BuildStructure
  ): Task[Seq[File]] = {
    val projects    = allApiProjects(projectRef.build, structure)
    val sourceTasks = projects.map { ref =>
      def taskFromProject[T](task: TaskKey[T]) = (ref / conf / task).get(structure.data)

      // Get all the Scala sources (not the Java ones)
      val filteredSources = taskFromProject(sources).map(_.map(_.filter(_.name.endsWith(extension))))

      // Join them
      val tasks = filteredSources.toSeq
      tasks.join.map(_.flatten)
    }
    sourceTasks.join.map(_.flatten)
  }

  /**
   * Get all projects in the given build that have `apiDocsInclude` set to `true`.
   * Recursively searches aggregated projects starting from the root project.
   */
  def allApiProjects(build: URI, structure: BuildStructure): Seq[ProjectRef] = {
    def aggregated(projectRef: ProjectRef): Seq[ProjectRef] = {
      val project   = Project.getProject(projectRef, structure)
      val childRefs = project.toSeq.flatMap(_.aggregate)
      childRefs.flatMap { childRef =>
        val includeApiDocs = (childRef / apiDocsInclude).get(structure.data).getOrElse(false)
        if (includeApiDocs) childRef +: aggregated(childRef) else aggregated(childRef)
      }
    }
    val rootProjectId  = structure.rootProject(build)
    val rootProjectRef = ProjectRef(build, rootProjectId)
    aggregated(rootProjectRef)
  }

  def allClasspaths(projectRef: ProjectRef, structure: BuildStructure): Task[Seq[File]] = {
    val projects = allApiProjects(projectRef.build, structure)
    // Full classpath is necessary to ensure that scaladoc and javadoc can see the compiled classes of the other language.
    val tasks = projects.flatMap { p => (p / Compile / fullClasspath).get(structure.data) }
    tasks.join.map(_.flatten.map(_.data).distinct)
  }

  // Note: webjars are extracted without versions
  def extractWebjarContents: Def.Initialize[Task[File]] = Def.task {
    val report    = update.value
    val targetDir = target.value

    val webjars     = report.matching(configurationFilter(name = Webjars.name))
    val webjarsDir  = targetDir / "webjars"
    val classLoader = new URLClassLoader(Path.toURLs(webjars), null)
    val extractor   = new WebJarExtractor(classLoader)
    extractor.extractAllWebJarsTo(webjarsDir)
    webjarsDir
  }
}
