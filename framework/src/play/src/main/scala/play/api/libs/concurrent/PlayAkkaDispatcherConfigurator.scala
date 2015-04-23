/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.concurrent

import com.typesafe.config.Config
import akka.dispatch._
import java.util.concurrent._
import scala.concurrent.duration._
import scala.concurrent.forkjoin.{ ForkJoinTask, ForkJoinPool }

/**
 * Clone of Akka's DispatcherConfigurator that adds the `async-mode`
 * option to the `fork-join-executor`.
 */
class PlayAkkaDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites)
    extends MessageDispatcherConfigurator(config, prerequisites) {

  private def durationConfig(key: String, tu: TimeUnit): FiniteDuration = {
    Duration(config.getDuration(key, tu), tu)
  }

  private val instance = new Dispatcher(
    this,
    config.getString("id"),
    config.getInt("throughput"),
    durationConfig("throughput-deadline-time", TimeUnit.NANOSECONDS),
    configureExecutor(),
    durationConfig("shutdown-timeout", TimeUnit.MILLISECONDS))

  override def dispatcher(): MessageDispatcher = instance

  final override def configureExecutor(): ExecutorServiceConfigurator = {
    def configurator(executor: String): ExecutorServiceConfigurator = executor match {
      case null | "" | "fork-join-executor" ⇒ new PlayAkkaForkJoinExecutorConfigurator(config.getConfig("fork-join-executor"), prerequisites)
      case "thread-pool-executor" ⇒ new ThreadPoolExecutorConfigurator(config.getConfig("thread-pool-executor"), prerequisites)
      case fqcn ⇒
        val args = List(
          classOf[Config] -> config,
          classOf[DispatcherPrerequisites] -> prerequisites)
        prerequisites.dynamicAccess.createInstanceFor[ExecutorServiceConfigurator](fqcn, args).recover({
          case exception ⇒ throw new IllegalArgumentException(
            ("""Cannot instantiate ExecutorServiceConfigurator ("executor = [%s]"), defined in [%s],
                make sure it has an accessible constructor with a [%s,%s] signature""")
              .format(fqcn, config.getString("id"), classOf[Config], classOf[DispatcherPrerequisites]), exception)
        }).get
    }

    config.getString("executor") match {
      case "default-executor" ⇒ new DefaultExecutorServiceConfigurator(config.getConfig("default-executor"), prerequisites, configurator(config.getString("default-executor.fallback")))
      case other ⇒ configurator(other)
    }
  }

}

object PlayAkkaForkJoinExecutorConfigurator {

  /**
   * Clone of Akka's AkkaForkJoinPool that supports the
   * asyncMode argument.
   */
  final class PlayAkkaForkJoinPool(
    parallelism: Int,
    threadFactory: ForkJoinPool.ForkJoinWorkerThreadFactory,
    unhandledExceptionHandler: Thread.UncaughtExceptionHandler,
    asyncMode: Boolean)
      extends ForkJoinPool(parallelism, threadFactory, unhandledExceptionHandler, asyncMode) /* not visible outside Akka: with LoadMetrics */ {
    override def execute(r: Runnable): Unit =
      if (r eq null) throw new NullPointerException else super.execute(new ForkJoinExecutorConfigurator.AkkaForkJoinTask(r))

    def atFullThrottle(): Boolean = this.getActiveThreadCount() >= this.getParallelism()
  }

}

/**
 * Clone of Akka's ForkJoinExecutorConfigurator that supports the `async-mode`
 * option.
 */
class PlayAkkaForkJoinExecutorConfigurator(config: Config, prerequisites: DispatcherPrerequisites) extends ExecutorServiceConfigurator(config, prerequisites) {
  import ForkJoinExecutorConfigurator._

  def validate(t: ThreadFactory): ForkJoinPool.ForkJoinWorkerThreadFactory = t match {
    case correct: ForkJoinPool.ForkJoinWorkerThreadFactory ⇒ correct
    case x ⇒ throw new IllegalStateException("The prerequisites for the ForkJoinExecutorConfigurator is a ForkJoinPool.ForkJoinWorkerThreadFactory!")
  }

  class ForkJoinExecutorServiceFactory(val threadFactory: ForkJoinPool.ForkJoinWorkerThreadFactory,
      val parallelism: Int,
      val asyncMode: Boolean) extends ExecutorServiceFactory {
    def createExecutorService: ExecutorService = new PlayAkkaForkJoinExecutorConfigurator.PlayAkkaForkJoinPool(
      parallelism,
      threadFactory,
      MonitorableThreadFactory.doNothing,
      asyncMode
    )
  }
  final override def createExecutorServiceFactory(id: String, threadFactory: ThreadFactory): ExecutorServiceFactory = {
    val tf = threadFactory match {
      case m: MonitorableThreadFactory ⇒
        // add the dispatcher id to the thread names
        m.withName(m.name + "-" + id)
      case other ⇒ other
    }
    // We actually provide a default under the 'akka' config path, but the user can set a
    // different config path.
    val asyncMode = if (config.hasPath("async-mode")) config.getBoolean("async-mode") else true
    new ForkJoinExecutorServiceFactory(
      threadFactory = validate(tf),
      parallelism = ThreadPoolConfig.scaledPoolSize(
        config.getInt("parallelism-min"),
        config.getDouble("parallelism-factor"),
        config.getInt("parallelism-max")),
      asyncMode = asyncMode
    )
  }
}
