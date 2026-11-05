/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The reverse routers have the same class names whether they are generated as Scala or as Java source code.
 */
class ReverseRouterTypesTest {
  @Test
  def preserveReverseRouterTypes(): Unit = {
    val reverse: controllers.ReverseApplication = controllers.routes.Application
    assertEquals("/root-prefixed-controller", reverse.rootPrefixedController.url)
    assertEquals("controllers.ReverseApplication", classOf[controllers.routes].getField("Application").getType.getName)

    val javascript: controllers.javascript.ReverseApplication = controllers.routes.javascript.Application
    assertEquals("controllers.Application.rootPrefixedController", javascript.rootPrefixedController.name)
    assertEquals(
      "controllers.javascript.ReverseApplication",
      classOf[controllers.routes.javascript].getField("Application").getType.getName
    )
  }
}
