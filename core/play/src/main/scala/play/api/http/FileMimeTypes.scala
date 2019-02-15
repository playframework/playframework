/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import java.util.Locale
import javax.inject.{ Inject, Provider, Singleton }

import scala.annotation.implicitNotFound

/**
 * Defines behavior for file type mappings.
 *
 * This trait is primarily used with results and assets that send files,
 * for users who want to send a file without having to specify an explicit
 * content type.  For example, a user can send a file with ".json":
 *
 * {{{
 * implicit val fileMimeTypes = ...
 * val file = new File("test.json")
 * Ok.sendFile(file) // <-- uses implicit fileMimeTypes
 * }}}
 *
 * and have a "json" -> "application/json" mapping done implicitly based
 * off the file extension.  The Assets controller handles this mapping
 * automatically.
 *
 * In a controller, an implicit FileMimeTypes object can either be defined
 * explicitly:
 *
 * {{{
 * class MyController @Inject()(implicit val fileMimeTypes: FileMimeTypes) extends BaseController {
 *    def sendFile() = ...
 * }
 * }}}
 *
 * or, if [[play.api.mvc.BaseController]] is extended, then
 * an implicit fileMimeTypes instance is already made available from
 * [[play.api.mvc.ControllerComponents]], meaning that no explicit import is required:
 *
 * {{{
 * class MyController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
 *   def sendFile() = ...
 * }
 * }}}
 */
@implicitNotFound("You do not have an implicit FileMimeTypes in scope. If you want to bring a FileMimeTypes into context, please use dependency injection.")
trait FileMimeTypes {
  /**
   * Retrieves the usual MIME type for a given file name
   *
   * @param name the file name, e.g. `hello.txt`
   * @return the MIME type, if defined
   */
  def forFileName(name: String): Option[String]

  /**
   * @return the Java version for this file mime types.
   */
  def asJava: play.mvc.FileMimeTypes = new play.mvc.FileMimeTypes(this)
}

@Singleton
class DefaultFileMimeTypesProvider @Inject() (fileMimeTypesConfiguration: FileMimeTypesConfiguration) extends Provider[FileMimeTypes] {
  lazy val get = new DefaultFileMimeTypes(fileMimeTypesConfiguration)
}

/**
 * Default implementation of FileMimeTypes.
 */
class DefaultFileMimeTypes @Inject() (config: FileMimeTypesConfiguration) extends FileMimeTypes {

  /**
   * Retrieves the usual MIME type for a given file name
   *
   * @param name the file name, e.g. `hello.txt`
   * @return the MIME type, if defined
   */
  override def forFileName(name: String): Option[String] = {
    name.split('.').takeRight(1).headOption.flatMap { ext =>
      config.mimeTypes.get(ext.toLowerCase(Locale.ENGLISH))
    }
  }

}
