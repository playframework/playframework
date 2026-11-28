/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The behavior of the reverse routers generated as Scala source code that differs from the Java ones.
 */
class ReverseRouterCompatibilityTest {
  @Test
  def createReverseRoutersWithAPrefix(): Unit = {
    assertEquals("/root-prefixed-controller", new controllers.ReverseApplication("/").rootPrefixedController.url)
    assertEquals(
      "controllers.Application.rootPrefixedController",
      new controllers.javascript.ReverseApplication("/").rootPrefixedController.name
    )
  }

  @Test
  def rejectUnmatchedReverseRouteArgumentsWithMatchError(): Unit = {
    assertThrows(classOf[MatchError], () => controllers.routes.Application.fixedOnlyParams("other"))
  }
}
