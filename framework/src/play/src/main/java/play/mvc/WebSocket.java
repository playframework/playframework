/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import akka.stream.javadsl.Flow;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import play.api.http.websocket.CloseCodes;
import play.http.websocket.Message;
import play.libs.F;
import play.libs.Scala;
import play.libs.streams.AkkaStreams;
import scala.PartialFunction;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * A WebSocket handler.
 */
public abstract class WebSocket {

    /**
     * Invoke the WebSocket.
     *
     * @param request The request for the WebSocket.
     * @return A future of either a result to reject the WebSocket connection with, or a Flow to handle the WebSocket.
     */
    public abstract CompletionStage<F.Either<Result, Flow<Message, Message, ?>>> apply(Http.RequestHeader request);

    /**
     * Acceptor for text WebSockets.
     */
    public static final MappedWebSocketAcceptor<String, String> Text = new MappedWebSocketAcceptor<>(Scala.partialFunction(message -> {
        if (message instanceof Message.Text) {
            return F.Either.Left(((Message.Text) message).data());
        } else if (message instanceof Message.Binary) {
            return F.Either.Right(new Message.Close(CloseCodes.Unacceptable(), "This websocket only accepts text frames"));
        } else {
            throw Scala.noMatch();
        }
    }), Message.Text::new);

    /**
     * Acceptor for binary WebSockets.
     */
    public static final MappedWebSocketAcceptor<ByteString, ByteString> Binary = new MappedWebSocketAcceptor<>(Scala.partialFunction(message -> {
        if (message instanceof Message.Binary) {
            return F.Either.Left(((Message.Binary) message).data());
        } else if (message instanceof Message.Text) {
            return F.Either.Right(new Message.Close(CloseCodes.Unacceptable(), "This websocket only accepts binary frames"));
        } else {
            throw Scala.noMatch();
        }
    }), Message.Binary::new);

    /**
     * Acceptor for JSON WebSockets.
     */
    public static final MappedWebSocketAcceptor<JsonNode, JsonNode> Json = new MappedWebSocketAcceptor<>(Scala.partialFunction(message -> {
        try {
            if (message instanceof Message.Binary) {
                return F.Either.Left(play.libs.Json.parse(((Message.Binary) message).data().iterator().asInputStream()));
            } else if (message instanceof Message.Text) {
                return F.Either.Left(play.libs.Json.parse(((Message.Text) message).data()));
            }
        } catch (RuntimeException e) {
            return F.Either.Right(new Message.Close(CloseCodes.Unacceptable(), "Unable to parse JSON message"));
        }
        throw Scala.noMatch();
    }), json -> new Message.Text(play.libs.Json.stringify(json)));

    /**
     * Acceptor for JSON WebSockets.
     *
     * @param in The class of the incoming messages, used to decode them from the JSON.
     * @param <In> The websocket's input type (what it receives from clients)
     * @param <Out> The websocket's output type (what it writes to clients)
     * @return The WebSocket acceptor.
     */
    public static <In, Out> MappedWebSocketAcceptor<In, Out> json(Class<In> in) {
        return new MappedWebSocketAcceptor<>(Scala.partialFunction(message -> {
            try {
                if (message instanceof Message.Binary) {
                    return F.Either.Left(play.libs.Json.mapper().readValue(((Message.Binary) message).data().iterator().asInputStream(), in));
                } else if (message instanceof Message.Text) {
                    return F.Either.Left(play.libs.Json.mapper().readValue(((Message.Text) message).data(), in));
                }
            } catch (Exception e) {
                return F.Either.Right(new Message.Close(CloseCodes.Unacceptable(), e.getMessage()));
            }
            throw Scala.noMatch();
        }), outMessage -> {
            try {
                return new Message.Text(play.libs.Json.mapper().writeValueAsString(outMessage));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Utility class for creating WebSockets.
     *
     * @param <In> the type the websocket reads from clients (e.g. String, JsonNode)
     * @param <Out> the type the websocket outputs back to remote clients (e.g. String, JsonNode)
     */
    public static class MappedWebSocketAcceptor<In, Out> {
        private final PartialFunction<Message, F.Either<In, Message>> inMapper;
        private final Function<Out, Message> outMapper;

        public MappedWebSocketAcceptor(PartialFunction<Message, F.Either<In, Message>> inMapper, Function<Out, Message> outMapper) {
            this.inMapper = inMapper;
            this.outMapper = outMapper;
        }

        /**
         * Accept a WebSocket.
         *
         * @param f A function that takes the request header, and returns a future of either the result to reject the
         *          WebSocket connection with, or a flow to handle the WebSocket messages.
         * @return The WebSocket handler.
         */
        public WebSocket acceptOrResult(Function<Http.RequestHeader, CompletionStage<F.Either<Result, Flow<In, Out, ? >>>> f) {
            return WebSocket.acceptOrResult(inMapper, f, outMapper);
        }

        /**
         * Accept a WebSocket.
         *
         * @param f A function that takes the request header, and returns a flow to handle the WebSocket messages.
         * @return The WebSocket handler.
         */
        public WebSocket accept(Function<Http.RequestHeader, Flow<In, Out, ? >> f) {
            return acceptOrResult(request -> CompletableFuture.completedFuture(F.Either.Right(f.apply(request))));
        }
    }

    /**
     * Helper to create handlers for WebSockets.
     *
     * @param inMapper Function to map input messages. If it produces left, the message will be passed to the WebSocket
     *                 flow, if it produces right, the message will be sent back out to the client - this can be used
     *                 to send errors directly to the client.
     * @param f The function to handle the WebSocket.
     * @param outMapper Function to map output messages.
     * @return The WebSocket handler.
     */
    private static <In, Out> WebSocket acceptOrResult(
            PartialFunction<Message, F.Either<In, Message>> inMapper,
            Function<Http.RequestHeader, CompletionStage<F.Either<Result, Flow<In, Out, ?>>>> f,
            Function<Out, Message> outMapper
    ) {
        return new WebSocket() {
            @Override
            public CompletionStage<F.Either<Result, Flow<Message, Message, ?>>> apply(Http.RequestHeader request) {
                return f.apply(request).thenApply(resultOrFlow -> {
                    if (resultOrFlow.left.isPresent()) {
                        return F.Either.Left(resultOrFlow.left.get());
                    } else {
                        Flow<Message, Message, ?> flow = AkkaStreams.bypassWith(
                                Flow.<Message>create().collect(inMapper),
                                play.api.libs.streams.AkkaStreams.onlyFirstCanFinishMerge(2),
                                resultOrFlow.right.get().map(outMapper::apply)
                        );
                        return F.Either.Right(flow);
                    }
                });
            }
        };
    }
}
