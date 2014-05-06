package scalaguide.tests.specs

import org.specs2.mock._
import org.specs2.mutable._

import java.util._

class ExampleMockitoSpec extends Specification with Mockito {

  case class Data(retrievalDate: java.util.Date)

  trait DataService {
    def findData: Data
  }

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

  // #scalaws-mockito
  "MyService#isDailyData" should {
    "return true if the data is from today" in {
      val mockDataService = mock[DataService]
      mockDataService.findData returns Data(retrievalDate = new java.util.Date())

      val myService = new MyService() {
        override def dataService = mockDataService
      }

      val actual = myService.isDailyData
      actual must equalTo(true)
    }
  }
  // #scalaws-mockito
}
