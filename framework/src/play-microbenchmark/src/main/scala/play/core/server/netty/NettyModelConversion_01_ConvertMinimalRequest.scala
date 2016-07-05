package play.core.server.netty

import io.netty.handler.codec.http.HttpRequest
import org.openjdk.jmh.annotations.{ TearDown, _ }
import play.api.mvc.RequestHeader

import scala.util.Try

@State(Scope.Benchmark)
class NettyModelConversion_01_ConvertMinimalRequest {

  // Cache some values that will be used in the benchmark
  private val conversion = NettyHelpers.conversion
  private val remoteAddress = NettyHelpers.localhost

  // Benchmark state
  private var request: HttpRequest = null
  private var result: RequestHeader = null

  @Setup(Level.Iteration)
  def setup(): Unit = {
    request = NettyHelpers.nettyRequest(
      method = "GET",
      target = "/",
      headers = Nil
    )
    result = null
  }

  @TearDown(Level.Iteration)
  def tearDown(): Unit = {
    // Sanity check the benchmark result
    assert(result.path == "/")
  }

  @Benchmark
  def convertRequest(): Unit = {
    result = conversion.convertRequest(1L, remoteAddress, None, request).get
  }
}