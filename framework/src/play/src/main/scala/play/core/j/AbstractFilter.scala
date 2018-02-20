/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import akka.stream.Materializer
import play.api.mvc.{ Filter => SFilter }
import play.mvc.{ EssentialFilter, Filter }

/**
 * This class is a Wrapper Class to get around the different trait Encodings
 * between Scala 2.11 and Scala 2.12
 *
 * @param materializer a simple Materializer
 * @param underlying the Filter that should be converted to scala
 */
private[play] abstract class AbstractFilter(materializer: Materializer, underlying: Filter) extends SFilter {

  override implicit def mat: Materializer = materializer

  override def asJava: EssentialFilter = underlying

}
