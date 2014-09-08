package scalaguide.tests.specs2

// #specs2-mockito
import org.specs2.mock._
import org.specs2.mutable._

import java.util._

class ExampleMockitoSpec extends Specification with Mockito {

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