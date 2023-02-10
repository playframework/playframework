/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.detailed.filters.csp

// #scala-csp-dynamic-action
package controllers {
  import javax.inject._

  import scala.concurrent.ExecutionContext

  import akka.stream.Materializer
  import play.api.mvc._
  import play.filters.csp._

  // Custom CSP action
  class AssetAwareCSPActionBuilder @Inject() (
      bodyParsers: PlayBodyParsers,
      cspConfig: CSPConfig,
      assetCache: AssetCache
  )(
      implicit protected override val executionContext: ExecutionContext,
      protected override val mat: Materializer
  ) extends CSPActionBuilder {
    override def parser: BodyParser[AnyContent] = bodyParsers.default

    // processor with dynamically generated config
    protected override def cspResultProcessor: CSPResultProcessor = {
      val modifiedDirectives: Seq[CSPDirective] = cspConfig.directives.map {
        case CSPDirective(name, value) if name == "script-src" =>
          CSPDirective(name, value + assetCache.cspDigests.mkString(" "))
        case csp: CSPDirective =>
          csp
      }

      CSPResultProcessor(CSPProcessor(cspConfig.copy(directives = modifiedDirectives)))
    }
  }

  // Dummy class that can have a dynamically changing list of csp-hashes
  class AssetCache {
    def cspDigests: Seq[String] = {
      Seq(
        "sha256-HELLO",
        "sha256-WORLD"
      )
    }
  }

  class HomeController @Inject() (cc: ControllerComponents, myCSPAction: AssetAwareCSPActionBuilder)
      extends AbstractController(cc) {
    def index = myCSPAction {
      Ok("I have an asset aware header!")
    }
  }
}

import com.google.inject.AbstractModule

class CSPModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[controllers.AssetCache]).asEagerSingleton()
    bind(classOf[controllers.AssetAwareCSPActionBuilder]).asEagerSingleton()
  }
}
// #scala-csp-dynamic-action
