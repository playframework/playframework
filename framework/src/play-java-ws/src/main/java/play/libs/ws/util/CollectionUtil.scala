/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws.util

import java.{ util =>ju }
import scala.collection.convert.WrapAsJava._

/** Utility class for converting a Scala `Map` with a nested collection type into its idiomatic Java counterpart. 
 *  The reason why this source is written in Scala is that doing the conversion using Java is a lot more involved. 
 *  This utility class is used by `play.libs.ws.StreamedResponse`.
 */
private[ws] object CollectionUtil {
  def convert(headers: Map[String, Seq[String]]): ju.Map[String, ju.List[String]] =
    mapAsJavaMap(headers.map { case (k, v) => k -> seqAsJavaList(v)})
}
