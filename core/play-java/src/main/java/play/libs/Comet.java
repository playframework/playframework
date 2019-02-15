/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import play.twirl.api.utils.StringEscapeUtils;

import java.util.Arrays;

/**
 * Provides an easy way to use a Comet formatted output with
 * <a href="http://doc.akka.io/docs/akka/2.5/java/stream/index.html">Akka Streams</a>.
 *
 * There are two methods that can be used to convert strings and JSON, {@code Comet.string}
 * and {@code Comet.json}.  These methods build on top of the base method, {@code Comet.flow},
 * which takes a Flow of {@code akka.util.ByteString} and organizes it into Comet format.
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
     * Produces a Flow of escaped ByteString from a series of String elements.  Calls
     * out to Comet.flow internally.
     *
     * @param callbackName the javascript callback method.
     * @return a flow of ByteString elements.
     */
    public static Flow<String, ByteString, NotUsed> string(String callbackName) {
        return Flow.of(String.class).map(str -> {
            return ByteString.fromString("'" + StringEscapeUtils.escapeEcmaScript(str) + "'");
        }).via(flow(callbackName));
    }

    /**
     * Produces a flow of ByteString using `Json.stringify` from a Flow of JsonNode.  Calls
     * out to Comet.flow internally.
     *
     * @param callbackName the javascript callback method.
     * @return a flow of ByteString elements.
     */
    public static Flow<JsonNode, ByteString, NotUsed> json(String callbackName) {
        return Flow.of(JsonNode.class).map(json -> {
            return ByteString.fromString(Json.stringify(json));
        }).via(flow(callbackName));
    }

    /**
     * Produces a flow of ByteString with a prepended block and a script wrapper.
     *
     * @param callbackName the javascript callback method.
     * @return a flow of ByteString elements.
     */
    public static Flow<ByteString, ByteString, NotUsed> flow(String callbackName) {
        ByteString cb = ByteString.fromString(callbackName);
        return Flow.of(ByteString.class).map((msg) -> {
            return formatted(cb, msg);
        }).prepend(Source.single(initialChunk));
    }

    private static ByteString formatted(ByteString callbackName, ByteString javascriptMessage) {
        ByteStringBuilder b = new ByteStringBuilder();
        b.append(ByteString.fromString("<script>"));
        b.append(callbackName);
        b.append(ByteString.fromString("("));
        b.append(javascriptMessage);
        b.append(ByteString.fromString(");</script>"));
        return b.result();
    }
}
