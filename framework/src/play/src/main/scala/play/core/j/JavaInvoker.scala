package play.core.j

import akka.actor._
import akka.actor.Actor._
import akka.routing._

import com.typesafe.config._

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.iteratee._
import play.api.http.HeaderNames._
import play.core.ApplicationProvider

import play.utils._

/**
 * holds Play's internal invokers
 */
class JavaInvoker(applicationProvider: Option[ApplicationProvider] = None) {

  val system: ActorSystem = applicationProvider.map { a =>
    JavaInvoker.appProviderActorSystem(a)
  }.getOrElse(ActorSystem("play"))

  /**
   * kills actor system
   */
  def stop(): Unit = {
    system.shutdown()
    system.awaitTermination()
  }

}

/**
 * provides Play's internal actor system and the corresponding actor instances
 */
object JavaInvoker {

  private var invokerOption: Option[JavaInvoker] = None

  private def invoker: JavaInvoker = invokerOption.getOrElse {
    val default = new JavaInvoker()
    invokerOption = Some(default)
    Logger.info("JavaInvoker was created outside of JavaInvoker#init - this potentially could lead to initialization problems in production mode")
    default
  }

  private def appProviderActorSystem(applicationProvider: ApplicationProvider) = {
    val conf = play.api.Play.maybeApplication.filter(_.mode == Mode.Prod).map(app =>
      ConfigFactory.load()).getOrElse(Configuration.loadDev(applicationProvider.path))
    ActorSystem("play", conf.getConfig("play"))
  }

  /**
   * contructor used by Server
   */
  def apply(applicationProvider: ApplicationProvider): JavaInvoker = new JavaInvoker(Some(applicationProvider))

  /**
   * saves invoker instance in global scope
   */
  def init(invoker: JavaInvoker): Unit = {
    if (invokerOption.isDefined)
      throw new IllegalStateException("JavaInvoker was initialized twice without an intervening uninit; two Server created at once?")
    invokerOption = Some(invoker)
  }

  /**
   * removes invoker instance from global scope
   */
  def uninit(): Unit = {
    invokerOption = None
  }

  /**
   * provides actor system
   */
  def system = invoker.system

}
