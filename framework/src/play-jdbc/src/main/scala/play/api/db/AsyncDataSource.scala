/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.db

import java.io.{ Closeable, PrintWriter }
import java.sql.{ Connection, SQLFeatureNotSupportedException }
import java.util.concurrent.{ BlockingQueue, CompletionStage, LinkedBlockingQueue, TimeUnit }
import java.util.logging.Logger
import javax.sql.DataSource

import play.db

import scala.annotation.tailrec
import scala.compat.java8.FutureConverters
import scala.concurrent.{ Future, Promise }
import scala.util.Try

trait AsyncDataSource extends DataSource with Closeable {
  top =>
  def getConnectionAsync: Future[Connection]
  def asJava: play.db.AsyncDataSource = new db.AsyncDataSource {
    override def getConnectionAsync: CompletionStage[Connection] = FutureConverters.toJava(top.getConnectionAsync)
    override def getConnection: Connection = top.getConnection
    override def getConnection(username: String, password: String): Connection = top.getConnection(username, password)
    override def setLoginTimeout(seconds: Int): Unit = top.setLoginTimeout(seconds)
    override def setLogWriter(out: PrintWriter): Unit = top.setLogWriter(out)
    override def getParentLogger: Logger = top.getParentLogger
    override def getLoginTimeout: Int = top.getLoginTimeout
    override def getLogWriter: PrintWriter = top.getLogWriter
    override def unwrap[T](iface: Class[T]): T = top.unwrap(iface)
    override def isWrapperFor(iface: Class[_]): Boolean = top.isWrapperFor(iface)
    override def close(): Unit = top.close
  }
}

object AsyncDataSource {

  /**
   * Wraps a `DataSource` to make an `AsyncDataSource`. The asynchrony is
   * achieved by requesting connections on a separate thread.
   *
   * @param dataSource The `DataSource` to wrap.
   * @param closeDataSource A `Closeable` object to call when closing the `DataSource`.
   *                        Usually this will just be the `dataSource` parameter repeated
   *                        again.
   * @return An `AsyncDataSource` that wraps the given `DataSource`.
   */
  def wrap(dataSource: DataSource, closeDataSource: Closeable): AsyncDataSource = {
    new Wrapper(dataSource, closeDataSource)
  }

  private[db] class Wrapper(val dataSource: DataSource, closeDataSource: Closeable) extends AsyncDataSource {
    private val service = new GetConnectionService(dataSource)

    override def getConnection: Connection = dataSource.getConnection
    override def getConnectionAsync: Future[Connection] = service.requestConnection()
    override def close(): Unit = {
      service.shutdown()
      closeDataSource.close()
    }

    override def getConnection(username: String, password: String): Connection = dataSource.getConnection(username, password)
    override def setLoginTimeout(seconds: Int): Unit = dataSource.setLoginTimeout(seconds)
    override def setLogWriter(out: PrintWriter): Unit = dataSource.setLogWriter(out)
    override def getParentLogger: Logger = dataSource.getParentLogger()
    override def getLoginTimeout: Int = dataSource.getLoginTimeout
    override def getLogWriter: PrintWriter = dataSource.getLogWriter
    override def unwrap[T](iface: Class[T]): T = {
      if (iface.isInstance(this)) {
        iface.cast(this)
      } else if (iface.isInstance(dataSource)) {
        iface.cast(dataSource)
      } else {
        dataSource.unwrap(iface)
      }
    }
    override def isWrapperFor(iface: Class[_]): Boolean = {
      iface.isInstance(this) || iface.isInstance(dataSource) || dataSource.isWrapperFor(iface)
    }
  }

  private[db] class GetConnectionService(dataSource: DataSource) {
    @volatile
    private var shutdownRequested: Boolean = false
    private val requests: BlockingQueue[Promise[Connection]] = new LinkedBlockingQueue[Promise[Connection]]()
    private val serviceThread: Thread = new Thread("AsyncDataSourceService") {
      override def run(): Unit = {
        // Block on the queue, waiting for requests to connections. If a c
        // is requested, then block to get a c and fulfil the promise. We
        // add a 50ms timeout when polling the queue so that we can periodically check
        // whether we need to shutdown.
        while (!shutdownRequested) {
          val request: Promise[Connection] = requests.poll(50, TimeUnit.MILLISECONDS)
          request match {
            case null => ()
            case promise => promise.complete(Try { dataSource.getConnection() })
          }
        }
        // Clear queue, completing any pending promises with an IllegalStateException.
        @tailrec
        def clearQueue(): Unit = {
          val request: Promise[Connection] = requests.poll()
          request match {
            case null => ()
            case promise =>
              promise.failure(new IllegalStateException("Can't get c: AsyncDataSourceService has been shutdown"))
              clearQueue()
          }
        }
        clearQueue()
      }
    }
    serviceThread.start()

    def requestConnection(): Future[Connection] = {
      if (shutdownRequested) {
        Future.failed(new IllegalStateException("Can't get c: AsyncDataSourceService has been shutdown"))
      } else {
        val p = Promise[Connection]()
        requests.add(p)
        p.future
      }
    }

    def shutdown(): Unit = { shutdownRequested = true }

  }
}