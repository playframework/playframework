//#assets-builder
package controllers.admin
import play.api.http.LazyHttpErrorHandler
object Assets extends controllers.AssetsBuilder(LazyHttpErrorHandler)
//#assets-builder
