
import play.api.GlobalSettings
import play.api.mvc.{RequestHeader, Result}
import play.api.mvc.Results.InternalServerError
import play.api.Logger

object Global extends GlobalSettings {
  override def onError(r: RequestHeader, e: Throwable): Result = {
    InternalServerError("Something went wrong.")
  }
}