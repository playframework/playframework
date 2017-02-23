/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringEscapeUtils;
import play.mvc.Results;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Provides an easy way to use a Comet formatted output with
 * <a href="http://doc.akka.io/docs/akka/current/java/stream/index.html">Akka Streams</a>.
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
public abstract class Comet extends Results.Chunks<String> {

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
        b.append(ByteString.fromString("<script type=\"text/javascript\">"));
        b.append(callbackName);
        b.append(ByteString.fromString("("));
        b.append(javascriptMessage);
        b.append(ByteString.fromString(");</script>"));
        return b.result();
    }

    //--------------------------------------------------------------------------------
    // Deprecated API follows
    //--------------------------------------------------------------------------------

    private Results.Chunks.Out<String> out;
    private String callbackMethod;

    /**
     * Create a new Comet socket.
     *
     * @deprecated Please use {@code Comet.string} or {@code Comet.json}, since 2.5.x
     * @param callbackMethod The Javascript callback method to call on each message.
     */
    @Deprecated
    public Comet(String callbackMethod) {
        super(play.core.j.JavaResults.writeString("text/html", play.api.mvc.Codec.javaSupported("utf-8")));
        this.callbackMethod = callbackMethod;
    }

    @Deprecated
    public void onReady(Results.Chunks.Out<String> out) {
        this.out = out;
        out.write(initialBuffer());
        onConnected();
    }

    /**
     * Initial chunk of data to send for browser compatibility (default to send 5Kb of blank data).
     */
    @Deprecated
    protected String initialBuffer() {
        char[] buffer = new char[1024 * 5];
        Arrays.fill(buffer, ' ');
        return new String(buffer);
    }

    /**
     * Send a message on this socket (will be received as String in the Javascript callback method).
     */
    @Deprecated
    public void sendMessage(String message) {
        out.write("<script type=\"text/javascript\">" + callbackMethod + "('" + org.apache.commons.lang3.StringEscapeUtils.escapeEcmaScript(message) + "');</script>");
    }

    /**
     * Send a Json message on this socket (will be received as Json in the Javascript callback method).
     */
    @Deprecated
    public void sendMessage(JsonNode message) {
        out.write("<script type=\"text/javascript\">" + callbackMethod + "(" + Json.stringify(message) + ");</script>");
    }

    /**
     * The socket is ready, you can start sending messages.
     */
    @Deprecated
    public abstract void onConnected();

    /**
     * Add a callback to be notified when the client has disconnected.
     */
    @Deprecated
    public void onDisconnected(Runnable callback) {
        out.onDisconnected(callback);
    }

    /**
     * Close the channel
     */
    @Deprecated
    public void close() {
        out.close();
    }

    /**
     * Creates a Comet. The abstract {@code onConnected} method is
     * implemented using the specified {@code Callback<Comet>} and
     * is invoked with {@code Comet.this}.
     *
     * @param jsMethod the Javascript method to call on each message
     * @param callback the callback used to implement onConnected
     * @return a new Comet
     * @throws NullPointerException if the specified callback is null
     */
    @Deprecated
    public static Comet whenConnected(String jsMethod, Consumer<Comet> callback) {
        return new WhenConnectedComet(jsMethod, callback);
    }

    /**
     * An extension of Comet that obtains its onConnected from
     * the specified {@code Callback<Comet>}.
     */
    static final class WhenConnectedComet extends Comet {

        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Comet.class);

        private final Consumer<Comet> callback;

        WhenConnectedComet(String jsMethod, Consumer<Comet> callback) {
            super(jsMethod);
            if (callback == null) throw new NullPointerException("Comet onConnected callback cannot be null");
            this.callback = callback;
        }

        @Override
        public void onConnected() {
            try {
                callback.accept(this);
            } catch (Throwable e) {
                logger.error("Exception in Comet.onConnected", e);
            }
        }
    }

}
