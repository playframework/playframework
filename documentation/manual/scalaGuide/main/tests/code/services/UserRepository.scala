/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests

package services

import models._

// #scalatest-repository
trait UserRepository {
  def roles(user:User) : Set[Role]
}
// #scalatest-repository


