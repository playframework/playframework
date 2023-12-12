/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import play.twirl.api.utils.StringEscapeUtils;

/**
 * Provides an easy way to use a Comet formatted output with <a
 * href="https://doc.akka.io/docs/akka/2.6/java/stream/index.html">Akka Streams</a>.
 *
 * <p>There are two methods that can be used to convert strings and JSON, {@code Comet.string} and
 * {@code Comet.json}. These methods build on top of the base method, {@code Comet.flow}, which
 * takes a Flow of {@code akka.util.ByteString} and organizes it into Comet format.
 *
 * <pre>{@literal
 *   public Result liveClock() {
 *        final DateTimeFormatter df = DateTimeFormatter.ofPattern("HH mm ss");
 *        final Source tickSource = Source.tick(Duration.Zero(), Duration.create(100, MILLISECONDS), "TICK");
 *        final Source eventSource = tickSource.map((tick) -> df.format(ZonedDateTime.now()));
 *
 *        final Source<ByteString, NotUsed> flow = eventSource.via(Comet.string("parent.clockChanged"));
 *        return ok().chunked(flow).as(Http.MimeTypes.HTML);
 *   }
 * }</pre>
 */
public abstract class Comet {

  private static ByteString initialChunk;

  static {
    char[] buffer = new char[1024 * 5];
    Arrays.fill(buffer, ' ');
    initialChunk = ByteString.fromString(new String(buffer) + "<html><body>");
  }

  /**
   * Produces a Flow of escaped ByteString from a series of String elements. Calls out to Comet.flow
   * internally.
   *
   * @param callbackName the javascript callback method.
   * @return a flow of ByteString elements.
   */
  public static Flow<String, ByteString, NotUsed> string(String callbackName) {
    return string(callbackName, null);
  }

  /**
   * Produces a Flow of escaped ByteString from a series of String elements. Calls out to Comet.flow
   * internally.
   *
   * @param callbackName the javascript callback method.
   * @param nonce The CSP nonce to use for the script tag. If {@code null} no nonce will be sent.
   * @return a flow of ByteString elements.
   */
  public static Flow<String, ByteString, NotUsed> string(String callbackName, String nonce) {
    return Flow.of(String.class)
        .map(
            str -> {
              return ByteString.fromString("'" + StringEscapeUtils.escapeEcmaScript(str) + "'");
            })
        .via(flow(callbackName, nonce));
  }

  /**
   * Produces a flow of ByteString using `Json.stringify` from a Flow of JsonNode. Calls out to
   * Comet.flow internally.
   *
   * @param callbackName the javascript callback method.
   * @return a flow of ByteString elements.
   */
  public static Flow<JsonNode, ByteString, NotUsed> json(String callbackName) {
    return json(callbackName, null);
  }

  /**
   * Produces a flow of ByteString using `Json.stringify` from a Flow of JsonNode. Calls out to
   * Comet.flow internally.
   *
   * @param callbackName the javascript callback method.
   * @param nonce The CSP nonce to use for the script tag. If {@code null} no nonce will be sent.
   * @return a flow of ByteString elements.
   */
  public static Flow<JsonNode, ByteString, NotUsed> json(String callbackName, String nonce) {
    return Flow.of(JsonNode.class)
        .map(
            json -> {
              return ByteString.fromString(Json.stringify(json));
            })
        .via(flow(callbackName, nonce));
  }

  /**
   * Produces a flow of ByteString with a prepended block and a script wrapper.
   *
   * @param callbackName the javascript callback method.
   * @return a flow of ByteString elements.
   */
  public static Flow<ByteString, ByteString, NotUsed> flow(String callbackName) {
    return flow(callbackName, null);
  }

  /**
   * Produces a flow of ByteString with a prepended block and a script wrapper.
   *
   * @param callbackName the javascript callback method.
   * @param nonce The CSP nonce to use for the script tag. If {@code null} no nonce will be sent.
   * @return a flow of ByteString elements.
   */
  public static Flow<ByteString, ByteString, NotUsed> flow(String callbackName, String nonce) {
    ByteString cb = ByteString.fromString(callbackName);
    return Flow.of(ByteString.class)
        .map(
            (msg) -> {
              return formatted(cb, msg, nonce);
            })
        .prepend(Source.single(initialChunk));
  }

  private static ByteString formatted(
      ByteString callbackName, ByteString javascriptMessage, String nonce) {
    ByteStringBuilder b = new ByteStringBuilder();
    b.append(ByteString.fromString("<script"));
    if (nonce != null && !nonce.isEmpty()) {
      b.append(ByteString.fromString(" nonce=\""));
      b.append(ByteString.fromString(nonce));
      b.append(ByteString.fromString("\""));
    }
    b.append(ByteString.fromString(">"));
    b.append(callbackName);
    b.append(ByteString.fromString("("));
    b.append(javascriptMessage);
    b.append(ByteString.fromString(");</script>"));
    return b.result();
  }
}
