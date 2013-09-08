package scalaguide.tests

package services

import models._

// #scalatest-repository
trait UserRepository {
  def roles(user:User) : Set[Role]
}
// #scalatest-repository


