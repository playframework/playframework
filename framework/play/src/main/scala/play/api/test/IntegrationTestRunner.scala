package play.api.test

import play.api.mvc._
import akka.actor.Actor
import akka.actor.Actor._
import akka.event.EventHandler
import play.core.server.NettyServer
import java.io._

case class Start(netty: Option[NettyServer])
case class Stop(netty: Option[NettyServer])

class RunnerActor extends Actor {

  def receive = {
    case Start(netty) =>
      netty.getOrElse(throw new Exception("at this point we should have netty running"))
    case Stop(netty) =>
      Thread.sleep(1000)
      println("shutting down")
      netty.getOrElse(throw new Exception("at this point we should have netty running")).stop()
      Actor.registry.shutdownAll()
  }
}

/**
 * provides an integration test runner.
 * It first fires up the application, then executes test, after the test is executed it kills Netty and the process ends
 * usage:
 * {{{
 * //in integrationtest folder
 * package test
 * object IntegrationTest extends IntegrationTestRunner {
 *  def test = {//here is my integration test}
 * }
 * }}}
 */
abstract class IntegrationTestRunner {

  /**
   * test to execute
   */
  def test(): Unit

  /**
   * main entry point for the integration test
   */
  def main(args: Array[String]) {
    val actor = actorOf[RunnerActor].start()
    val netty = Option(System.getProperty("user.dir")).map(new File(_)).filter(p => p.exists && p.isDirectory).map(applicationPath => NettyServer.createServer(applicationPath)).getOrElse(throw new Exception("there is no valid play app in the directory"))

    actor ! Start(netty)

    val cleanRun = try {
      test()
      None
    } catch { case ex: Exception => Some(ex) }

    actor ! Stop(netty)

    Thread.sleep(300)

    if (cleanRun.isDefined) throw cleanRun.get
  }
}

