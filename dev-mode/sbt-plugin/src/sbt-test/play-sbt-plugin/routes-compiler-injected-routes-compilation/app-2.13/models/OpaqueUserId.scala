/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package models

import play.api.mvc.PathBindable
import play.api.mvc.QueryStringBindable

// Scala 2 has no opaque types. This keeps the fixture cross-building; the opaque type
// variant that this test is about lives in app-3.
case class OpaqueUserId(id: String) extends AnyVal

object OpaqueUserId {
  implicit val pathBindable: PathBindable[OpaqueUserId] =
    PathBindable.bindableString.transform(OpaqueUserId(_), _.id)

  implicit val queryStringBindable: QueryStringBindable[OpaqueUserId] =
    QueryStringBindable.bindableString.transform(OpaqueUserId(_), _.id)
}
