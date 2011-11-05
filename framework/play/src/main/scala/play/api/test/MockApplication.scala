package play.api.test

import java.io.File
import play.api._
import play.core._
import collection.JavaConverters._

/**
 * provides a mock implementation of an application
 * example:
 * {{{
 * object  MyMock extends MockApplication {
 *  val mockConfig = withMockDataSources
 *  val mockPlugins = Nil
 * }
 * }}}
 * and then before accessing any play.api._
 * {{{
 * implicit val currentApp = MyMock.mockApp
 * }}}
 */
abstract class Mock {
  private[test] lazy val cl = new ApplicationClassLoader(Thread.currentThread().getContextClassLoader())

  private[test] val loadDefaultPlugins = new Application(new File("."), cl, None, Play.Mode.Dev).plugins

  private[test] val mockData = Map("application.name" -> "mock app",
    "db.default.driver" -> "org.h2.Driver",
    "db.default.url" -> "jdbc:h2:mem:play",
    "ebean.default" -> "models.*")

  /**
   * provides mockdata for java API
   */
  def mockDataAsJava: java.util.Map[String, String] = mockData.asJava

  /**
   * creates an app with defined mock config and mock plugins
   * @param mockPlugins
   * @param mockConfig
   * @return mocked application
   */
  def makeApp(mockPlugins: java.util.List[Plugin], mockConfig: java.util.Map[String, String]): Application = new Application(new File("."), cl, None, Play.Mode.Dev) {
    override lazy val configuration = new Configuration(mockConfig.asScala.toMap.map(item => item._1 -> Configuration.Config(item._1, item._2, new File("."))))
    override val plugins: Seq[Plugin] = mockPlugins.asScala.toSeq ++ loadDefaultPlugins
  }
  /*
   * provides a way to set the current application, useful only from java apps
   */
  def setCurrentApp(app: Application) {
    Play._currentApp = app
  }
}

trait MockApplication extends Mock {

  /**
   * provides predefined in memory db config
   */
  def withMockDataSources = mockData

  /**
   * placeholder for user supplied mock configuration
   */
  val mockConfig: Map[String, String]

  /**
   * placeholder for user supplied mock plugins
   */
  val mockPlugins: Seq[Plugin]

  /*
   * provides a way to set the current application, useful only from java apps
   */
  lazy val mockApp: Application = makeApp(mockPlugins.toList.asJava, mockConfig.asJava)

  Play.start(mockApp)
}
