/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.routes.compiler

import java.io.File

import play.routes.compiler.RoutesCompiler.RoutesCompilerTask

trait RoutesGenerator {

  /**
   * Generate a router
   *
   * @param task The routes compile task
   * @param namespace The namespace of the router
   * @param rules The routing rules
   * @return A sequence of output filenames to file contents
   */
  def generate(task: RoutesCompilerTask, namespace: Option[String], rules: List[Rule]): Seq[(String, String)]

  /**
   * An identifier for this routes generator.
   *
   * May include configuration if applicable.
   *
   * Used for incremental compilation to tell if the routes generator has changed (and therefore a new compile needs
   * to be done).
   */
  def id: String
}

private object RoutesGenerator {
  val ForwardsRoutesFile          = "Routes.scala"
  val ReverseRoutesFile           = "ReverseRoutes.scala"
  val JavaScriptReverseRoutesFile = "JavaScriptReverseRoutes.scala"
  val RoutesPrefixFile            = "RoutesPrefix.scala"
  val JavaWrapperFile             = "routes.java"
}

/**
 * A routes generator that generates dependency injected routers
 */
object InjectedRoutesGenerator extends RoutesGenerator {
  import RoutesGenerator._

  val id = "injected"

  case class Dependency[+T <: Rule](ident: String, clazz: String, rule: T)

  def generate(task: RoutesCompilerTask, namespace: Option[String], rules: List[Rule]): Seq[(String, String)] = {
    val folder = namespace.map(_.replace('.', '/') + "/").getOrElse("") + "/"

    val sourceInfo =
      RoutesSourceInfo(
        new File(".").toURI.relativize(task.file.toURI).getPath.replace(File.separator, "/"),
        new java.util.Date().toString
      )
    val routes = rules.collect { case r: Route => r }

    val routesPrefixFiles = Seq(folder + RoutesPrefixFile -> generateRoutesPrefix(sourceInfo, namespace))

    val forwardsRoutesFiles = if (task.forwardsRouter) {
      Seq(folder + ForwardsRoutesFile -> generateRouter(sourceInfo, namespace, task.additionalImports, rules))
    } else {
      Nil
    }

    val reverseRoutesFiles = if (task.reverseRouter) {
      generateReverseRouters(sourceInfo, namespace, task.additionalImports, routes, task.namespaceReverseRouter) ++
        generateJavaScriptReverseRouters(
          sourceInfo,
          namespace,
          task.additionalImports,
          routes,
          task.namespaceReverseRouter
        ) ++
        generateJavaWrappers(sourceInfo, namespace, rules, task.namespaceReverseRouter)
    } else {
      Nil
    }

    routesPrefixFiles ++ forwardsRoutesFiles ++ reverseRoutesFiles
  }

  private def generateRouter(
      sourceInfo: RoutesSourceInfo,
      namespace: Option[String],
      additionalImports: Seq[String],
      rules: List[Rule]
  ) = {
    @annotation.tailrec
    def prepare(
        rules: List[Rule],
        includes: Seq[Include],
        routes: Seq[Route]
    ): (Seq[Include], Seq[Route]) = rules match {
      case (inc: Include) :: rs =>
        prepare(rs, inc +: includes, routes)

      case (rte: Route) :: rs =>
        prepare(rs, includes, rte +: routes)

      case _ => includes.reverse -> routes.reverse
    }

    val (includes, routes) = prepare(rules, Seq.empty, Seq.empty)

    // Generate dependency descriptors for all includes
    val includesDeps: Map[String, Dependency[Include]] =
      includes
        .groupBy(_.router)
        .zipWithIndex
        .flatMap {
          case ((router, includes), index) =>
            includes.headOption.map { inc =>
              router -> Dependency(router.replace('.', '_') + "_" + index, router, inc)
            }
        }
        .toMap

    // Generate dependency descriptors for all routes
    val routesDeps: Map[(Option[String], String, Boolean), Dependency[Route]] =
      routes
        .groupBy { r =>
          (r.call.packageName, r.call.controller, r.call.instantiate)
        }
        .zipWithIndex
        .flatMap {
          case ((key @ (packageName, controller, instantiate), routes), index) =>
            routes.headOption.map { route =>
              val clazz = packageName.map(_ + ".").getOrElse("") + controller
              // If it's using the @ syntax, we depend on the provider (ie, look it up each time)
              val dep   = if (instantiate) s"javax.inject.Provider[$clazz]" else clazz
              val ident = controller + "_" + index

              key -> Dependency(ident, dep, route)
            }
        }
        .toMap

    // Get the distinct dependency descriptors in the same order as defined in the routes file
    val orderedDeps = rules.map {
      case include: Include =>
        includesDeps(include.router)
      case route: Route =>
        routesDeps((route.call.packageName, route.call.controller, route.call.instantiate))
    }.distinct

    // Map all the rules to dependency descriptors
    val rulesWithDeps = rules.map {
      case include: Include =>
        includesDeps(include.router).copy(rule = include)
      case route: Route =>
        routesDeps((route.call.packageName, route.call.controller, route.call.instantiate)).copy(rule = route)
    }

    inject.twirl
      .forwardsRouter(
        sourceInfo,
        namespace,
        additionalImports,
        orderedDeps,
        rulesWithDeps,
        includesDeps.values.toSeq
      )
      .body
  }

  private def generateRoutesPrefix(sourceInfo: RoutesSourceInfo, namespace: Option[String]) =
    static.twirl
      .routesPrefix(
        sourceInfo,
        namespace,
        _ => true
      )
      .body

  private def generateReverseRouters(
      sourceInfo: RoutesSourceInfo,
      namespace: Option[String],
      additionalImports: Seq[String],
      routes: List[Route],
      namespaceReverseRouter: Boolean
  ) = {
    routes.groupBy(_.call.packageName.map(_.stripPrefix("_root_."))).map {
      case (pn, routes) =>
        val packageName = namespace
          .filter(_ => namespaceReverseRouter)
          .map(_ + pn.map("." + _).getOrElse(""))
          .orElse(pn.orElse(namespace))
        (packageName.map(_.replace(".", "/") + "/").getOrElse("") + ReverseRoutesFile) ->
          static.twirl
            .reverseRouter(
              sourceInfo,
              namespace,
              additionalImports,
              packageName,
              routes,
              namespaceReverseRouter,
              _ => true
            )
            .body
    }
  }

  private def generateJavaScriptReverseRouters(
      sourceInfo: RoutesSourceInfo,
      namespace: Option[String],
      additionalImports: Seq[String],
      routes: List[Route],
      namespaceReverseRouter: Boolean
  ) = {
    routes.groupBy(_.call.packageName.map(_.stripPrefix("_root_."))).map {
      case (pn, routes) =>
        val packageName = namespace
          .filter(_ => namespaceReverseRouter)
          .map(_ + pn.map("." + _).getOrElse(""))
          .orElse(pn.orElse(namespace))
        (packageName.map(_.replace(".", "/") + "/").getOrElse("") + "javascript/" + JavaScriptReverseRoutesFile) ->
          static.twirl
            .javascriptReverseRouter(
              sourceInfo,
              namespace,
              additionalImports,
              packageName,
              routes,
              namespaceReverseRouter,
              _ => true
            )
            .body
    }
  }

  private def generateJavaWrappers(
      sourceInfo: RoutesSourceInfo,
      namespace: Option[String],
      rules: List[Rule],
      namespaceReverseRouter: Boolean
  ) = {
    rules.collect { case r: Route => r }.groupBy(_.call.packageName.map(_.stripPrefix("_root_."))).map {
      case (pn, routes) =>
        val packageName = namespace
          .filter(_ => namespaceReverseRouter)
          .map(_ + pn.map("." + _).getOrElse(""))
          .orElse(pn.orElse(namespace))
        val controllers = routes.groupBy(_.call.controller).keys.toSeq

        (packageName.map(_.replace(".", "/") + "/").getOrElse("") + JavaWrapperFile) ->
          static.twirl.javaWrappers(sourceInfo, namespace, packageName, controllers).body
    }
  }
}
