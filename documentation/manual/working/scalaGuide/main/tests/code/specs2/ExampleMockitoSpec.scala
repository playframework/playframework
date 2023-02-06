/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.tests.specs2

// #specs2-mockito
import java.util._

import org.mockito.Mockito._
import org.specs2.mutable._

class ExampleMockitoSpec extends Specification {
  "MyService#isDailyData" should {
    "return true if the data is from today" in {
      val mockDataService = mock(classOf[DataService])
      when(mockDataService.findData).thenReturn(Data(retrievalDate = new java.util.Date()))

      val myService = new MyService() {
        override def dataService = mockDataService
      }

      val actual = myService.isDailyData
      actual must equalTo(true)
    }
  }
}
// #specs2-mockito

// #specs2-mockito-dataservice
trait DataService {
  def findData: Data
}

case class Data(retrievalDate: java.util.Date)
// #specs2-mockito-dataservice

class MyService {
  def dataService: DataService = null // implementation reference...

  def isDailyData: Boolean = {
    val retrievalDate = Calendar.getInstance
    retrievalDate.setTime(dataService.findData.retrievalDate)

    val today = Calendar.getInstance()

    (retrievalDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    && retrievalDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR))
  }
}
