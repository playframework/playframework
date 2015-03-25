/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
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

/**
 * A routes generator that generates static routers
 */
object StaticRoutesGenerator extends RoutesGenerator {

  val id = "static"

  def generate(task: RoutesCompilerTask, namespace: Option[String], rules: List[Rule]): Seq[(String, String)] = {

    val filePrefix = namespace.map(_.replace('.', '/') + "/").getOrElse("") + "/routes"

    val sourceInfo = RoutesSourceInfo(task.file.getCanonicalPath.replace(File.separator, "/"), new java.util.Date().toString)
    val routes = rules.collect { case r: Route => r }

    val forwardsRoutesFiles = if (task.forwardsRouter) {
      Seq(filePrefix + "_routing.scala" -> generateRouter(sourceInfo, namespace, task.additionalImports, rules))
    } else {
      Nil
    }

    val reverseRoutesFiles = if (task.reverseRouter) {
      Seq(filePrefix + "_reverseRouting.scala" -> generateReverseRouter(sourceInfo, namespace, task.additionalImports,
        routes, task.namespaceReverseRouter)) ++
        generateJavaWrappers(sourceInfo, namespace, rules, task.namespaceReverseRouter)
    } else {
      Nil
    }

    forwardsRoutesFiles ++ reverseRoutesFiles
  }

  private def generateRouter(sourceInfo: RoutesSourceInfo, namespace: Option[String], additionalImports: Seq[String], rules: List[Rule]) =
    static.twirl.forwardsRouter(
      sourceInfo,
      namespace,
      additionalImports,
      rules
    ).body

  private def generateReverseRouter(sourceInfo: RoutesSourceInfo, namespace: Option[String], additionalImports: Seq[String], routes: List[Route], namespaceReverseRouter: Boolean) =
    static.twirl.reverseRouters(
      sourceInfo,
      namespace,
      additionalImports,
      routes,
      namespaceReverseRouter,
      _.call.instantiate
    ).body

  private def generateJavaWrappers(sourceInfo: RoutesSourceInfo, namespace: Option[String], rules: List[Rule], namespaceReverseRouter: Boolean) = {
    rules.collect { case r: Route => r }.groupBy(_.call.packageName).map {
      case (pn, routes) =>
        val packageName = namespace.filter(_ => namespaceReverseRouter).map(_ + "." + pn).getOrElse(pn)
        val controllers = routes.groupBy(_.call.controller).keys.toSeq

        (packageName.replace(".", "/") + "/routes.java") ->
          static.twirl.javaWrappers(sourceInfo, namespace, packageName, controllers).body
    }
  }
}

/**
 * A routes generator that generates dependency injected routers
 */
object InjectedRoutesGenerator extends RoutesGenerator {

  val id = "injected"

  case class Dependency[+T <: Rule](ident: String, clazz: String, rule: T)

  def generate(task: RoutesCompilerTask, namespace: Option[String], rules: List[Rule]): Seq[(String, String)] = {

    val filePrefix = namespace.map(_.replace('.', '/') + "/").getOrElse("") + "/routes"

    val sourceInfo = RoutesSourceInfo(task.file.getCanonicalPath.replace(File.separator, "/"), new java.util.Date().toString)
    val routes = rules.collect { case r: Route => r }

    val forwardsRoutesFiles = if (task.forwardsRouter) {
      Seq(filePrefix + "_routing.scala" -> generateRouter(sourceInfo, namespace, task.additionalImports, rules))
    } else Nil

    val reverseRoutesFiles = if (task.reverseRouter) {
      Seq(filePrefix + "_reverseRouting.scala" -> generateReverseRouter(sourceInfo, namespace, task.additionalImports,
        routes, task.namespaceReverseRouter)) ++
        generateJavaWrappers(sourceInfo, namespace, rules, task.namespaceReverseRouter)
    } else {
      Nil
    }

    forwardsRoutesFiles ++ reverseRoutesFiles
  }

  private def generateRouter(sourceInfo: RoutesSourceInfo, namespace: Option[String], additionalImports: Seq[String], rules: List[Rule]) = {

    // Generate dependency descriptors for all includes
    val includesDeps = rules.collect {
      case include: Include => include
    }.groupBy(_.router).zipWithIndex.map {
      case ((router, includes), index) =>
        router -> Dependency(router.replace('.', '_') + "_" + index, router, includes.head)
    }.toMap

    // Generate dependency descriptors for all routes
    val routesDeps = rules
      .collect { case route: Route => route }
      .groupBy(r => (r.call.packageName, r.call.controller, r.call.instantiate))
      .zipWithIndex.map {
        case ((key @ (packageName, controller, instantiate), routes), index) =>
          val clazz = packageName + "." + controller
          // If it's using the @ syntax, we depend on the provider (ie, look it up each time)
          val dep = if (instantiate) s"javax.inject.Provider[$clazz]" else clazz
          val ident = controller + "_" + index
          key -> Dependency(ident, dep, routes.head)
      }.toMap

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

    inject.twirl.forwardsRouter(
      sourceInfo,
      namespace,
      additionalImports,
      orderedDeps,
      rulesWithDeps,
      includesDeps.values.toSeq
    ).body
  }

  private def generateReverseRouter(sourceInfo: RoutesSourceInfo, namespace: Option[String], additionalImports: Seq[String], routes: List[Route], namespaceReverseRouter: Boolean) =
    static.twirl.reverseRouters(
      sourceInfo,
      namespace,
      additionalImports,
      routes,
      namespaceReverseRouter,
      _ => true
    ).body

  private def generateJavaWrappers(sourceInfo: RoutesSourceInfo, namespace: Option[String], rules: List[Rule], namespaceReverseRouter: Boolean) = {
    rules.collect { case r: Route => r }.groupBy(_.call.packageName).map {
      case (pn, routes) =>
        val packageName = namespace.filter(_ => namespaceReverseRouter).map(_ + "." + pn).getOrElse(pn)
        val controllers = routes.groupBy(_.call.controller).keys.toSeq

        (packageName.replace(".", "/") + "/routes.java") ->
          static.twirl.javaWrappers(sourceInfo, namespace, packageName, controllers).body
    }
  }
}
