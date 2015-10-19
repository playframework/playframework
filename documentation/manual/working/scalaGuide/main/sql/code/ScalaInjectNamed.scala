package scalaguide.sql

import javax.inject.Inject
import play.api.db.{ Database, NamedDatabase }
import play.api.mvc.Controller

// inject "orders" database instead of "default"
class ScalaInjectNamed @Inject()(
  @NamedDatabase("orders") db: Database) extends Controller {
  // do whatever you need with the db
}
