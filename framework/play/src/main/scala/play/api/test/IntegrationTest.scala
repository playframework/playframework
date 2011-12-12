package play.api.test

import play.api.mvc._
import akka.actor.Actor
import akka.actor.Actor._
import akka.event.EventHandler
import play.core.server.NettyServer
import play.core.StaticApplication
import java.io._

case class Start(netty: Option[NettyServer])
case class Stop(netty: Option[NettyServer])

private[test] class RunnerActor extends Actor {

  def receive = {
    case Start(netty) =>
      netty.getOrElse(throw new Exception("at this point we should have netty running"))
    case Stop(netty) =>
      play.api.Play.stop()
      netty.getOrElse(throw new Exception("at this point we should have netty running")).stop()
      Actor.registry.shutdownAll()
  }
}

/**
 * provides a simple way to execute Webdriver/selenium tests.
 * example:
 * {{{
 * withNettyServer{
 *  val driver = new HtmlUnitDriver()
 *  driver.get("http://localhost:9000")
 *  driver.getPageSource must contain ("Hello world")
 * }
 * by default, HtmlUnitDriver and ChromeDriver will be in scope
 * }}}
 *
 */
object IntegrationTest {
  def run(r: Runnable) = () => r.run()

  /**
   * runs the given block in a real play application context
   */
  def withNettyServer(test: => Unit) = {
    val actor = actorOf[RunnerActor].start()
    val netty = Option(System.getProperty("user.dir")).map(new File(_)).filter(p => p.exists && p.isDirectory).map(applicationPath =>
      new NettyServer(new StaticApplication(applicationPath), "0.0.0.0", 9000))

    actor ! Start(netty)

    Thread.sleep(100)

    val cleanRun = try {
      test
      None
    } catch {
      case ex: Exception => Some(ex)
      case _ => None
    }

    actor ! Stop(netty)

    Thread.sleep(300)

    if (cleanRun.isDefined) throw cleanRun.get
    else true
  }
}
