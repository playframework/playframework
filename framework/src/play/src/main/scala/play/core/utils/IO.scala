package play.core.utils

// Taken from slide 21 of Martin Odersky's http://www.scala-lang.org/node/1261
// please move to util.
trait IO {
  /** Manage the usage of some object that must be closed after use */
  def use[R <: { def close() }, T](resource: R)(f: R => T): T = try {
    f(resource)
  } finally {
    if (resource != null) resource.close()
  }
}

object IO extends IO