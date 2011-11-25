package play.api.test

import java.io.File
import play.api._
import play.core._
import collection.JavaConverters._

/**
 * provides standard mock data
 */
object MockData {
  val dataSource = Map("application.name" -> "mock app",
    "db.default.driver" -> "org.h2.Driver",
    "db.default.url" -> "jdbc:h2:mem:play",
    "ebean.default" -> "models.*",
    "mock" -> "true")
  def dataSourceAsJava = dataSource.asJava

}

/*
 * provides underlying mock implementation
 */
abstract class Mock {
  private[test] lazy val cl = new ApplicationClassLoader(Thread.currentThread().getContextClassLoader())

  /**
   * creates an app with defined mock config and mock plugins
   * @param mockPlugins
   * @param mockConfig
   * @return mocked application
   */
  def createMock(mockPlugins: java.util.List[String], mockConfig: java.util.Map[String, String]): Application = {
    new Application(new File("."), cl, None, Play.Mode.Dev) {
      override val configuration = new Configuration(mockConfig.asScala.toMap.map(item => item._1 -> Configuration.Config(item._1, item._2, new File("."))))
      override val dynamicPlugins: List[String] = mockPlugins.asScala.toList
    }
  }
  /**
   * clears mock from context
   */
  def clearMock() {
    Play.stop()
  }

  /**
   * set passed application into global context
   */
  def setCurrentApp(app: Application) {
    if (Play.maybeApplication.isDefined == false) Play.start(app)
  }

}
/**
 * provides a mock runner.
 * there are two ways to use it
 * 1) inject a global mock into the context
 * 2) use provied control structure to manage app life cycle during testing
 * example:
 * {{{
 *    withApplication(Nil, MockData.dataSource) {
 *      myvariable must contain ("hello")
 *    }
 * }}}
 */
trait MockApplication extends Mock {
  /**
   * executes given specs block in a mock, play app context
   * @param mockPlugins
   * @param mockconfig
   * @param block
   */
  def withApplication(mockPlugins: List[String], mockConfig: Map[String, String])(f: => Unit): Boolean = {
    val mockApp = injectGlobalMock(Nil, MockData.dataSource)
    try {
      f
      clearMock()
    } catch {
      case ex: Exception =>
        clearMock()
        throw ex
    }
    true
  }

  /**
   * creates a global application mock and sets into global context.
   * @param mockPlugins
   * @param mockConfig
   * @return mock app
   */
  def injectGlobalMock(mockPlugins: List[String], mockConfig: Map[String, String]) = {
    val mockApp = createMock(mockPlugins.asJava, mockConfig.asJava)
    setCurrentApp(mockApp)
    mockApp
  }

}
/**
 * object representation of MockApplication trait
 */
object MockApplication extends MockApplication
