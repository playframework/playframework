/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests

import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play._

import org.mockito.Mockito._
import java.util._

// #scalaws-mockitosugar
class ExampleMockitoSugarSpec extends PlaySpec with MockitoSugar {

  "MyService#isDailyData" should {
    "return true if the data is from today" in {
      val mockDataService = mock[DataService]
      when(mockDataService.findData) thenReturn Data(new Date)

      val myService = new MyService() {
        override def dataService = mockDataService
      }

      val actual = myService.isDailyData
      actual mustBe true
    }
  }
}
// #scalaws-mockitosugar
