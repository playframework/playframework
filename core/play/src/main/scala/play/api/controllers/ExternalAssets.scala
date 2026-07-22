/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import java.io._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import jakarta.inject.Inject
import play.api._
import play.api.http.FileMimeTypes
import play.api.mvc._

/**
 * Controller that serves static resources from an external folder.
 * It is useful in development mode if you want to serve static assets that shouldn't be part of the build process.
 *
 * Note that this controller IS NOT intended to be used in production mode and can lead to security issues.
 * Therefore it is automatically disabled in production mode.
 *
 * Requested paths are normalized lexically and must remain within the configured root. Symbolic links are not
 * resolved by this containment check, so a link below the root may point outside it. This is intentional to support
 * linked development assets; the configured asset tree must therefore be trusted.
 *
 * All assets are served with max-age=3600 cache directive.
 *
 * You can use this controller in any application, just by declaring the appropriate route. For example:
 * {{{
 * GET     /assets/\uFEFF*file               controllers.ExternalAssets.at(path="/home/peter/myplayapp/external", file)
 * GET     /assets/\uFEFF*file               controllers.ExternalAssets.at(path="C:\external", file)
 * GET     /assets/\uFEFF*file               controllers.ExternalAssets.at(path="relativeToYourApp", file)
 * }}}
 */
class ExternalAssets @Inject() (environment: Environment)(implicit ec: ExecutionContext, fileMimeTypes: FileMimeTypes)
    extends ControllerHelpers {
  val AbsolutePath = """^(/|[a-zA-Z]:\\).*""".r

  private val Action = new ActionBuilder.IgnoringBody()(using _root_.controllers.Execution.trampoline)

  /**
   * Generates an `Action` that serves a static resource from an external folder
   *
   * @param rootPath the root folder for searching the static resource files such as `"/home/peter/public"`, `C:\external` or `relativeToYourApp`
   * @param file the file part extracted from the URL
   */
  def at(rootPath: String, file: String): Action[AnyContent] = Action.async { request =>
    environment.mode match {
      case Mode.Prod => Future.successful(NotFound)
      case _         =>
        Future {
          val root = rootPath match {
            case AbsolutePath(_) => new File(rootPath)
            case _               => environment.getFile(rootPath)
          }

          ExternalAssets.resolveFile(root, file).filter(_.exists) match {
            case Some(fileToServe) =>
              Ok.sendFile(fileToServe, inline = true).withHeaders(CACHE_CONTROL -> "max-age=3600")
            case None => NotFound
          }
        }
    }
  }
}

private[controllers] object ExternalAssets {
  private[controllers] def resolveFile(root: File, file: String): Option[File] = {
    val normalizedRoot = root.toPath.toAbsolutePath.normalize
    val resolvedFile   = normalizedRoot.resolve(file).normalize
    Option.when(resolvedFile.startsWith(normalizedRoot))(resolvedFile.toFile)
  }
}
