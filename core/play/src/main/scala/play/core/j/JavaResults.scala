/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import scala.annotation.varargs
import scala.language.reflectiveCalls

import play.mvc.{ ResponseHeader => JResponseHeader }

object JavaResultExtractor {
  @varargs
  def withHeader(responseHeader: JResponseHeader, nameValues: String*): JResponseHeader = {
    import scala.jdk.CollectionConverters._
    if (nameValues.length % 2 != 0) {
      throw new IllegalArgumentException(
        "Unmatched name - withHeaders must be invoked with an even number of string arguments"
      )
    }
    val toAdd = nameValues.grouped(2).map(pair => pair(0) -> pair(1))
    responseHeader.withHeaders(toAdd.toMap.asJava)
  }
}
