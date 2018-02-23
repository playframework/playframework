/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import play.mvc.{ ResponseHeader => JResponseHeader }

import scala.annotation.varargs
import scala.collection.JavaConverters
import scala.language.reflectiveCalls

object JavaResultExtractor {

  @varargs
  def withHeader(responseHeader: JResponseHeader, nameValues: String*): JResponseHeader = {
    import JavaConverters._
    if (nameValues.length % 2 != 0) {
      throw new IllegalArgumentException("Unmatched name - withHeaders must be invoked with an even number of string arguments")
    }
    val toAdd = nameValues.grouped(2).map(pair => pair(0) -> pair(1))
    responseHeader.withHeaders(toAdd.toMap.asJava)
  }

}
