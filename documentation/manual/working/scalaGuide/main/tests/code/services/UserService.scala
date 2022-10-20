/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
