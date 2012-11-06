
import play.api.mvc._
import play.api._

import play.filters.csrf._

object Global extends WithFilters(CSRFFilter()) with GlobalSettings