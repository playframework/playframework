/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package models

import play.api.mvc.PathBindable
import play.api.mvc.QueryStringBindable

opaque type OpaqueUserId = String

object OpaqueUserId {
  def apply(id: String): OpaqueUserId = id

  extension (userId: OpaqueUserId) def id: String = userId

  given pathBindable: PathBindable[OpaqueUserId] =
    PathBindable.bindableString.transform(OpaqueUserId.apply, _.id)

  given queryStringBindable: QueryStringBindable[OpaqueUserId] =
    QueryStringBindable.bindableString.transform(OpaqueUserId.apply, _.id)
}
