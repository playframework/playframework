/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.microbenchmark.it

import java.util.concurrent.TimeUnit

import okhttp3.{ OkHttpClient, Protocol, Request, Response }
import org.openjdk.jmh.annotations._
import play.api.mvc.Results
import play.api.test.{ ApplicationFactory, ServerEndpointRecipe }
import play.core.server.ServerEndpoint
import play.microbenchmark.it.HelloWorldBenchmark.ThreadState

import scala.util.Random

/**
 * Benchmark which starts a full Play server that returns "Hello world" responses.
 * The server is started in several different configurations to test different backends and protocols.
 * The OkHttp client is used for testing because it supports HTTP/2. One client instance
 * is used per thread. This
 */
@Threads(64)
@State(Scope.Benchmark)
class HelloWorldBenchmark {

  /** Which type of backend and connection to use. */
  @Param(Array("nt-11-pln", "nt-11-enc", "ak-11-pln", "ak-11-enc", "ak-20-enc"))
  var endpoint: String = null

  /** How many requests to make on a connection before closing it and making a new connection. */
  @Param(Array("5"))
  var reqsPerConn: String = null

  /** The backend and connection to use. */
  var serverEndpoint: ServerEndpoint = null
  /** A handle to close the server. */
  var endpointCloseable: AutoCloseable = null

  @Setup(Level.Trial)
  def setup(): Unit = {
    val appFactory = ApplicationFactory.withResult(Results.Ok("Hello world"))
    val endpointRecipe = endpoint match {
      case "nt-11-pln" => ServerEndpointRecipe.Netty11Plaintext
      case "nt-11-enc" => ServerEndpointRecipe.Netty11Plaintext
      case "ak-11-pln" => ServerEndpointRecipe.AkkaHttp11Plaintext
      case "ak-11-enc" => ServerEndpointRecipe.AkkaHttp11Encrypted
      case "ak-20-enc" => ServerEndpointRecipe.AkkaHttp20Encrypted

    }
    val startResult = ServerEndpointRecipe.startEndpoint(endpointRecipe, appFactory)
    serverEndpoint = startResult._1
    endpointCloseable = startResult._2
  }

  @TearDown(Level.Trial)
  def tearDown(): Unit = {
    endpointCloseable.close()
  }

  @Benchmark
  def helloWorld(threadState: ThreadState): Unit = {
    threadState.helloWorld()
  }

}

object HelloWorldBenchmark {

  /**
   * Contains state used by each thread in the benchmark. Each thread
   * has its own HTTP client. This means there's less contention between
   * threads when making requests, at the expense of higher overall
   * resource use. The clients' connection pools are cleared periodically
   * so that the time to open connections is included in the benchmark.
   */
  @State(Scope.Thread)
  class ThreadState {
    /** Used to make requests. */
    private var client: OkHttpClient = null
    /** A pre-built request; reused since they're identical. */
    private var request: Request = null
    /** How many requests to make before closing a connection. */
    private var reqsPerConn: Int = 0
    /** Which request we're currently on. Starts with a random value. */
    private var reqsPerConnCount: Int = 0

    /** The protocol we expect to find when we connect to the server. */
    private var expectedProtocol: Protocol = null
    /** The protocol we got with the last response from the server. */
    private var responseProtocol: Protocol = null
    /** The body we got with the last response from the server. */
    private var responseBody: String = null

    @Setup(Level.Trial)
    def setup(bench: HelloWorldBenchmark): Unit = {
      // Build the client
      client = {
        val Timeout = 5
        val b1 = new OkHttpClient.Builder()
          .connectTimeout(Timeout, TimeUnit.SECONDS)
          .readTimeout(Timeout, TimeUnit.SECONDS)
          .writeTimeout(Timeout, TimeUnit.SECONDS)
        // Add SSL options if we need to
        val b2 = bench.serverEndpoint.ssl match {
          case Some(ssl) =>
            b1.sslSocketFactory(ssl.sslContext.getSocketFactory, ssl.trustManager)
          case _ => b1
        }
        b2.build()
      }
      // Pre-build the request
      request = new Request.Builder().url(bench.serverEndpoint.pathUrl("/")).build()
      // Store the expected protocol so we can verify we're testing the correct HTTP version
      expectedProtocol = if (bench.serverEndpoint.expectedHttpVersions.contains("2")) {
        Protocol.HTTP_2
      } else if (bench.serverEndpoint.expectedHttpVersions.contains("1.1")) {
        Protocol.HTTP_1_1
      } else {
        throw new IllegalArgumentException("Server endpoint must support either HTTP version 1.1 or 2")
      }
      reqsPerConn = Integer.parseInt(bench.reqsPerConn)
      reqsPerConnCount = Random.nextInt(reqsPerConn) // Randomize slightly to decorrelate
    }

    @TearDown(Level.Trial)
    def tearDown(): Unit = {
      client.dispatcher().executorService().shutdown()
      client.connectionPool().evictAll()
      // Sanity check of result - this doesn't check every result, just the last result that was returned
      assert(responseProtocol == expectedProtocol)
      assert(responseBody == "Hello world")
    }

    /** Called by each thread in helloWorld benchmark */
    def helloWorld(): Unit = {
      val response: Response = client.newCall(request).execute()

      // Store results so they can be checked during teardown
      responseProtocol = response.protocol
      responseBody = response.body.string

      // Increment our request count and close the connection if needed
      reqsPerConnCount += 1
      if (reqsPerConn > reqsPerConn) {
        reqsPerConnCount = 0
        client.connectionPool().evictAll() // This closes the single connection in the pool
      }
    }

  }

}
