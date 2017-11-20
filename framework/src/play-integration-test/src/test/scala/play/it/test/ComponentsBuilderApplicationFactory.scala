package play.it.test

import play.api._
import play.api.http.{ DefaultHttpErrorHandler, HttpErrorHandler }
import play.api.mvc.{ Action, DefaultActionBuilder, Result, Results }
import play.api.routing.Router

/**
 * Helpers for the [[ComponentsBuilderApplicationFactory]] class.
 */
object ComponentsBuilderApplicationFactory {

  /**
   * The default `Router` used by the default factory.
   */
  val DefaultRouter: BuiltInComponents => Router = { components: BuiltInComponents =>
    Router.from {
      case _ => components.defaultActionBuilder { Results.Ok }
    }
  }

  /**
   * The default factory.
   */
  val Default = new ComponentsBuilderApplicationFactory(
    extraConfig = Map.empty,
    createRouter = DefaultRouter,
    createErrorHandler = None
  )

}

/**
 * A class that holds the components used to make an `Application` out of
 * a `BuiltinComponents` object. This allows users to use a builder pattern
 * to make a `BuiltinComponents` object.
 */
class ComponentsBuilderApplicationFactory private[ComponentsBuilderApplicationFactory] (
    val extraConfig: Map[String, Any],
    val createRouter: BuiltInComponents => Router,
    val createErrorHandler: Option[BuiltInComponents => HttpErrorHandler]
) extends ApplicationFactory {

  /**
   * Create an application from the components.
   */
  override def create(): Application = {
    val context = ApplicationLoader.Context.create(
      environment = Environment.simple(),
      initialSettings = Map[String, AnyRef](Play.GlobalAppConfigKey -> java.lang.Boolean.FALSE) ++ extraConfig.asInstanceOf[Map[String, AnyRef]]
    )
    val components = new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {
      override lazy val router: Router = createRouter(this)
      override lazy val httpErrorHandler: HttpErrorHandler = {
        createErrorHandler match {
          case None =>
            new DefaultHttpErrorHandler(environment, configuration, sourceMapper, Some(router))
          case Some(createErrorHandler) =>
            createErrorHandler(this)
        }
      }
    }
    components.application
  }

  def withExtraConfig(extraConfig: Map[String, Any]): ComponentsBuilderApplicationFactory = new ComponentsBuilderApplicationFactory(
    extraConfig = extraConfig,
    createRouter = createRouter,
    createErrorHandler = createErrorHandler
  )
  def withExtraConfig(extraConfig: (String, Any)*): ComponentsBuilderApplicationFactory = withExtraConfig(extraConfig: _*)

  def withRouter(createRouter: BuiltInComponents => Router): ComponentsBuilderApplicationFactory = new ComponentsBuilderApplicationFactory(
    extraConfig = extraConfig,
    createRouter = createRouter,
    createErrorHandler = createErrorHandler
  )
  def withAction(createAction: BuiltInComponents => Action[_]): ComponentsBuilderApplicationFactory = withRouter { components: BuiltInComponents =>
    val action = createAction(components)
    val router = Router.from { case _ => action }
    router
  }
  def withResult(createResult: BuiltInComponents => Result): ComponentsBuilderApplicationFactory = withAction { components: BuiltInComponents =>
    components.defaultActionBuilder { createResult(components) }
  }
  def withResult(result: => Result): ComponentsBuilderApplicationFactory = withAction(_.defaultActionBuilder(result))
  def withOk(message: String): ComponentsBuilderApplicationFactory = withResult(Results.Ok(message))
  def withOk: ComponentsBuilderApplicationFactory = withResult(Results.Ok)

  def withErrorHandler(createErrorHandler: BuiltInComponents => HttpErrorHandler): ComponentsBuilderApplicationFactory = new ComponentsBuilderApplicationFactory(
    extraConfig = extraConfig,
    createRouter = createRouter,
    createErrorHandler = Some(createErrorHandler)
  )
}
