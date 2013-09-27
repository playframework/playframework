import org.apache.ivy.core.install.InstallOptions
import org.apache.ivy.core.report.ResolveReport
import org.apache.ivy.core.resolve.IvyNode
import org.apache.ivy.plugins.matcher.PatternMatcher
import org.apache.ivy.util.filter.FilterHelper
import scala.collection.JavaConverters._
import sbt._
import Keys._

// NOTE - PRONOUNCED REPOSITORY BOUGHDA
object RepositoryBuilder {
  val localRepoProjectsPublished = TaskKey[Unit]("local-repo-projects-published", "Ensures local projects are published before generating the local repo.")
  val localRepoArtifacts = SettingKey[Seq[ModuleID]]("local-repository-artifacts", "Artifacts included in the local repository.")
  val localRepoName = "install-to-local-repository"
  val localRepo = SettingKey[File]("local-repository", "The location to install a local repository.")
  val localRepoCreated = TaskKey[LocalRepoReport]("local-repository-created", "Creates a local repository in the specified location.")
  val licenseReport = TaskKey[Unit]("local-repository-licenses")

  case class LocalRepoReport(localRepo: File, licenses: Map[String, Seq[License]])

  def addProjectsToRepository(projects: Seq[ProjectReference]): Setting[_] =
    localRepoArtifacts <++= (projects map { ref =>
        // The annoyance caused by cross-versioning.
        (Keys.projectID in ref, Keys.ivyScala in ref) apply {
          (id, ivy) =>
            CrossVersion(id, ivy) match {
              case Some(f) =>
                id.copy(name = f(id.name), explicitArtifacts = id.explicitArtifacts map (a => a.copy(name = f(a.name))))
              case _ => id
        }
        }
      }).join
  
  def localRepoCreationSettings: Seq[Setting[_]] = Seq(
    Keys.fullResolvers <<= (Keys.externalResolvers, Keys.sbtResolver) map (_ :+ _),
    localRepo <<= target apply (_ / "local-repository"),
    localRepoArtifacts := Seq.empty,
    resolvers <+= localRepo apply { f => Resolver.file(localRepoName, f)(Resolver.ivyStylePatterns) },
    localRepoProjectsPublished := (),
    //localRepoProjectsPublished <<= (ThePlayBuild.publishedProjects map (publishLocal in _)).dependOn,
    localRepoCreated <<= (localRepo, localRepoArtifacts, ivySbt, streams, localRepoProjectsPublished) map { (r, m, i, s, _) =>
      // TODO - Hook to detect if we need to recreate the repository....
      // That way we don't have to clean all the time.
      LocalRepoReport(r, createLocalRepository(m, localRepoName, i, s.log))
    },
    licenseReport <<= (localRepoCreated, state) map { (r, s) => doLicenseReport(r) }
  )

  case class License(name: String, url: String)(val deps: Seq[String]) {
    override def toString = name + " @ " + url
  }

  /** Resolves a set of modules from an SBT configured ivy and pushes them into
    * the given repository (by name).
    *
    * Intended usage, requires the named resolve to exist, and be on that accepts installed artifacts (i.e. file://)
    */
  def createLocalRepository( modules: Seq[ModuleID],
                             localRepoName: String,
                             ivy: IvySbt,
                             log: Logger): Map[String, Seq[License]] = ivy.withIvy(log) { ivy =>

    // This helper method installs a particular module and transitive dependencies.
    def installModule(module: ModuleID): Option[ResolveReport] = {
      // TODO - Use SBT's default ModuleID -> ModuleRevisionId
      val mrid = IvySbtCheater toID module
      val name = ivy.getResolveEngine.getSettings.getResolverName(mrid)
      log.debug("Module: " + mrid + " should use resolver: " + name)

      // This is a bit of a hack, scala-compiler is published under the "master" configuration, so we set the
      // configuration to it for master in the localRepoArtifacts setting.
      val configurations = (module.configurations.toSeq :+ "runtime").toArray
      try Some(ivy.install(mrid, name, localRepoName,
        new InstallOptions()
          .setTransitive(true)
          .setValidate(true)
          .setOverwrite(true)
          .setMatcherName(PatternMatcher.EXACT)
          .setArtifactFilter(FilterHelper.NO_FILTER)
          .setConfs(configurations)
      ))
      catch {
        case e: Exception =>
          log.debug("Failed to resolve module: " + module)
          log.trace(e)
          None
      }
    }
    // Grab all Artifacts
    val reports = (modules.distinct flatMap installModule).toSeq

    val projects = reports.flatMap { report =>
      // The first dependency is the module
      val deps = report.getDependencies.asInstanceOf[java.util.List[IvyNode]].asScala
      val module = deps.head
      if (module.getId.getOrganisation == "com.typesafe.play") {
        Seq(module.getId.getName.takeWhile(_ != '_') -> deps)
      } else Nil
    }.toMap

    projects.mapValues { deps =>
      for {
        dep <- deps
        if dep != null && dep.getId.getOrganisation != "com.typesafe.play"
        desc <- Option(dep.getDescriptor).toSeq
        license <- Option(desc.getLicenses).filterNot(_.isEmpty).map(_.map(l => l.getName -> l.getUrl)) getOrElse Array("None specified" -> "n/a")
      } yield License(license._1, license._2)(dep.getAllArtifacts.map(a => a.getModuleRevisionId.getOrganisation + ":" + a.getModuleRevisionId.getName))
    }
  }

  def doLicenseReport(localRepoReport: LocalRepoReport): Unit = {

    case class Category(name: String, safe: Boolean)

    // First, get all licenses
    val allLicenses = localRepoReport.licenses.values.toSeq.flatten.map(_.name).distinct
    // License grouping heuristics
    val categories = allLicenses.map(l => l -> l.toLowerCase).groupBy {
      case bsd if bsd._2.contains("bsd") => Category("BSD", true)
      case apache if apache._2.contains("apache") || apache._2.contains("asf") => Category("Apache", true)
      case lgpl if lgpl._2.contains("lpgl") || lgpl._2.contains("lesser general public license") => Category("LGPL", false)
      case gplClasspath if (gplClasspath._2.contains("gpl") || gplClasspath._2.contains("general public license"))
        && gplClasspath._2.contains("classpath") => Category("GPL with classpath exception", false)
      case gpl if gpl._2.contains("gpl") || gpl._2.contains("general public license") => Category("GPL", false)
      case mozilla if mozilla._2.contains("mozilla") || mozilla._2.contains("mpl") => Category("Mozilla", true)
      case mit if mit._2.contains("mit") => Category("MIT", true)
      case public if public._2.contains("public domain") => Category("Public domain", true)
      case none if none._2 == "none specified" => Category(none._1, false)
      case other => Category("Other", true)
    }.map(kv => kv._1 -> kv._2.map(_._1))

    val reverseCategories = categories.toSeq.flatMap { c =>
      c._2.map(_ -> c._1)
    }.toMap

    println("# Play License Report")
    println()

    println("## License categories")
    println()
    println("The following license categories have been created:")
    println()
    println("## Safe licenses")
    println()
    categories.filter(_._1.safe).foreach { category =>
      println("* " + category._1.name)
      category._2.foreach(l => println("  * " + l))
    }
    println()
    println("## Unsafe licenses")
    println()
    categories.filterNot(_._1.safe).foreach { category =>
      println("* " + category._1.name)
      category._2.foreach(l => println("  * " + l))
    }
    println()

    println("## Projects")
    println()

    val categorisedProjects = localRepoReport.licenses.groupBy {
      case ("play" | "play-java" | "play-iteratees" | "play-functional" | "play-datacommons" | "play-json" | "play-exceptions" | "templates", _) => "core"
      case ("sbt-plugin" | "templates-compiler" | "routes-compiler" | "console" | "sbt-link", _) => "dev"
      case ("play-test", _) => "test"
      case ("play-integration-test" | "play-docs", _) => "ignore"
      case _ => "module"
    }

    def formatProjects(projects: Map[String, Seq[License]]) = {

      println("Projects: " + projects.map(_._1).mkString(", "))
      println()

      val byCategory = projects.values.toSeq.flatten.groupBy(l => reverseCategories(l.name))

      def formatDeps(byCategory: Map[Category, Seq[License]]) {
        byCategory.flatMap { c =>
          c._2.flatMap(l => l.deps.map(_ -> l.name))
        }.toSeq.sortBy(_._1).foreach { dep =>
          println("* " + dep._1 + " (" + dep._2 + ")")
        }
      }

      println("#### Unsafe dependencies")
      println()
      formatDeps(byCategory.filterNot(_._1.safe))
      println()
      println("#### Safe dependencies")
      println()
      formatDeps(byCategory.filter(_._1.safe))
      println()
    }

    println("### Play core dependencies")
    println()
    println("Play core dependencies are the dependencies of the Play core libraries that must be included with either every Scala or every Java project.")
    println()
    formatProjects(categorisedProjects("core"))
    println()

    println("### Play development dependencies")
    println()
    println("Play development dependencies are the dependencies of the Play SBT plugins and associated libraries that only get used during project development time.  That is, not the development of Play itself, but a customer will use it when they are developing their Play application.")
    println()
    formatProjects(categorisedProjects("dev"))
    println()

    println("### Play test dependencies")
    println()
    println("Play test dependencies are the dependencies of the Play test framework and associated libraries that only get used during project testing.  That is, not the testing of Play itself, but a customer will use it when they are writing and running tests for their Play application.")
    println()
    formatProjects(categorisedProjects("test"))
    println()

    println("### Play modules dependencies")
    println()
    println("Play modules dependencies are the dependencies of the optional Play modules framework and associated libraries that only get used if a customer chooses to use these modules.  Note that these some of these modules are quite fundamental to the average application, for example, one module is play-jdbc, which provides basic database connectivity.  Every project that uses a relational database will use this.")
    println()
    formatProjects(categorisedProjects("module"))
    println()
  }

}

// THIS PIECE OF HACKERY IS AMAZING.
// We need access to "toID" which is private[sbt].  If SBT changes, this may need to change, but
// it has been stable for a while.
package sbt {
  object IvySbtCheater {
    // Converts from SBT module id to Ivy's version of a module ID so we can use the Ivy library directly.
    def toID(m: ModuleID) = IvySbt toID m
  }
}