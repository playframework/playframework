/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.db.Database
import play.api.test._

class HomeControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "access the database which requires evolutions to run" in {
    val database = inject[Database]
    val value = database.withConnection(connection => {
      val rs = connection
        .prepareStatement("select * from table1")
        .executeQuery()
      rs.next() mustBe true
      rs.getString(1)
    })
    value mustBe "hello"
  }
}
