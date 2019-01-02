/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import scala.collection.convert._

/**
 * Implicit conversions for use in the templates, to provide seamless interop between Java and Scala types.
 */
private[play] trait JavaImplicitConversions extends WrapAsJava with WrapAsScala
