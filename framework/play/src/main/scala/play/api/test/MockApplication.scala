package play.api.test

import java.io.File
import play.api._
import play.core._
import collection.JavaConverters._

abstract class Mock {
  private[test] lazy val cl = new ApplicationClassLoader(Thread.currentThread().getContextClassLoader())

  private[test] val loadDefaultPlugins = new Application(new File("."), cl, None, Play.Mode.Dev).plugins

  private[test] val mockData = Map("application.name" -> "mock app",
    "db.default.driver" -> "org.h2.Driver",
    "db.default.url" -> "jdbc:h2:mem:play",
    "ebean.default" -> "models.*")

  def mockDataAsJava: java.util.Map[String, String] = mockData.asJava

  def makeApp(mockPlugins: java.util.List[Plugin], mockConfig: java.util.Map[String, String]): Application = new Application(new File("."), cl, None, Play.Mode.Dev) {
    override lazy val configuration = new Configuration(mockConfig.asScala.toMap.map(item => item._1 -> Configuration.Config(item._1, item._2, new File("."))))
    override val plugins: Seq[Plugin] = mockPlugins.asScala.toSeq ++ loadDefaultPlugins
  }
  def setCurrentApp(app: Application) {
    Play._currentApp = app
  }
}

trait MockApplication extends Mock {

  def withMockDataSources = mockData

  val mockConfig: Map[String, String]

  val mockPlugins: Seq[Plugin]

  implicit lazy val mockApp: Application = makeApp(mockPlugins.toList.asJava, mockConfig.asJava)

  Play.start(mockApp)
}
