/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.tests

package services

import models._

// #scalatest-repository
trait UserRepository {
  def roles(user:User) : Set[Role]
}
// #scalatest-repository


