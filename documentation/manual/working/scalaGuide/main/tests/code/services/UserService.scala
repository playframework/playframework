/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests

package services

import models._

// #scalatest-userservice
class UserService(userRepository : UserRepository) {

  def isAdmin(user:User) : Boolean = {
    userRepository.roles(user).contains(Role("ADMIN"))
  }
}
// #scalatest-userservice
