/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.tests

package services

import models._

// #scalatest-userservice
class UserService(userRepository: UserRepository) {

  def isAdmin(user: User): Boolean = {
    userRepository.roles(user).contains(Role("ADMIN"))
  }
}
// #scalatest-userservice
