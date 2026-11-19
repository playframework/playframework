/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routes.compiler

import org.specs2.mutable.Specification

/**
 * Tools that read routes files use the routes model, so these examples only compile while its shape stays the same.
 */
class RoutesModelsSpec extends Specification {
  private case object CustomPart extends PathPart

  "the routes model" should {
    "match parameters by their four fields" in {
      Parameter("id", "Long", None, Some("1")) match {
        case Parameter(name, typeName, fixed, default) =>
          (name, typeName, fixed, default) must_== (("id", "Long", None, Some("1")))
      }
    }

    "accept path parts defined outside the routes compiler" in {
      PathPattern(Seq(CustomPart)).parts must_== Seq(CustomPart)
    }
  }
}
