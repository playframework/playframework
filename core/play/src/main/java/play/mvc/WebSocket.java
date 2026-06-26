/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.util.ByteString;
import play.api.http.websocket.CloseCodes;
import play.http.websocket.Message;
import play.libs.F;
import play.libs.Scala;
import play.libs.streams.PekkoStreams;
import scala.PartialFunction;

/** A WebSocket handler. */
public abstract class WebSocket {

  /**
   * Invoke the WebSocket.
   *
   * @param request The request for the WebSocket.
   * @return A future of either a result to reject the WebSocket connection with, or a Flow to
   *     handle the WebSocket.
   */
  public abstract CompletionStage<F.Either<Result, Flow<Message, Message, ?>>> apply(
      Http.RequestHeader request);

  /**
   * Invoke the WebSocket, including WebSocket handshake metadata.
   *
   * @param request The request for the WebSocket.
   * @return A future of either a result to reject the WebSocket connection with, or an accepted
   *     WebSocket with the flow and handshake metadata.
   */
  public CompletionStage<F.Either<Result, Accepted<Message, Message>>> applyWithOptions(
      Http.RequestHeader request) {
    return apply(request)
        .thenApply(
            resultOrFlow -> {
              if (resultOrFlow.left.isPresent()) {
                return F.Either.Left(resultOrFlow.left.get());
              } else {
                return F.Either.Right(
                    new Accepted<>(resultOrFlow.right.get(), firstRequestedSubprotocol(request)));
              }
            });
  }

  /** Acceptor for WebSockets to directly handle Play's Message objects. */
  public static final MappedWebSocketAcceptor<Message, Message> Message =
      new WebSocket.MappedWebSocketAcceptor<>(
          Scala.partialFunction(message -> F.Either.Left(message)), Function.identity());

  /** Acceptor for text WebSockets. */
  public static final MappedWebSocketAcceptor<String, String> Text =
      new MappedWebSocketAcceptor<>(
          Scala.partialFunction(
              message -> {
                if (message instanceof Message.Text) {
                  return F.Either.Left(((Message.Text) message).data());
                } else if (message instanceof Message.Binary) {
                  return F.Either.Right(
                      new Message.Close(
                          CloseCodes.Unacceptable(), "This websocket only accepts text frames"));
                } else {
                  throw Scala.noMatch();
                }
              }),
          Message.Text::new);

  /** Acceptor for binary WebSockets. */
  public static final MappedWebSocketAcceptor<ByteString, ByteString> Binary =
      new MappedWebSocketAcceptor<>(
          Scala.partialFunction(
              message -> {
                if (message instanceof Message.Binary) {
                  return F.Either.Left(((Message.Binary) message).data());
                } else if (message instanceof Message.Text) {
                  return F.Either.Right(
                      new Message.Close(
                          CloseCodes.Unacceptable(), "This websocket only accepts binary frames"));
                } else {
                  throw Scala.noMatch();
                }
              }),
          Message.Binary::new);

  /** Acceptor for JSON WebSockets. */
  public static final MappedWebSocketAcceptor<JsonNode, JsonNode> Json =
      new MappedWebSocketAcceptor<>(
          Scala.partialFunction(
              message -> {
                try {
                  if (message instanceof Message.Binary) {
                    return F.Either.Left(
                        play.libs.Json.parse(((Message.Binary) message).data().asInputStream()));
                  } else if (message instanceof Message.Text) {
                    return F.Either.Left(play.libs.Json.parse(((Message.Text) message).data()));
                  }
                } catch (RuntimeException e) {
                  return F.Either.Right(
                      new Message.Close(CloseCodes.Unacceptable(), "Unable to parse JSON message"));
                }
                throw Scala.noMatch();
              }),
          json -> new Message.Text(play.libs.Json.stringify(json)));

  /**
   * Acceptor for JSON WebSockets.
   *
   * @param in The class of the incoming messages, used to decode them from the JSON.
   * @param <In> The websocket's input type (what it receives from clients)
   * @param <Out> The websocket's output type (what it writes to clients)
   * @return The WebSocket acceptor.
   */
  public static <In, Out> MappedWebSocketAcceptor<In, Out> json(Class<In> in) {
    return new MappedWebSocketAcceptor<>(
        Scala.partialFunction(
            message -> {
              try {
                if (message instanceof Message.Binary) {
                  return F.Either.Left(
                      play.libs.Json.mapper()
                          .readValue(((Message.Binary) message).data().asInputStream(), in));
                } else if (message instanceof Message.Text) {
                  return F.Either.Left(
                      play.libs.Json.mapper().readValue(((Message.Text) message).data(), in));
                }
              } catch (Exception e) {
                return F.Either.Right(new Message.Close(CloseCodes.Unacceptable(), e.getMessage()));
              }
              throw Scala.noMatch();
            }),
        outMessage -> {
          try {
            return new Message.Text(play.libs.Json.mapper().writeValueAsString(outMessage));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  /**
   * An accepted WebSocket, including the flow that handles WebSocket messages and optional
   * handshake metadata.
   *
   * @param <In> the type the websocket reads from clients
   * @param <Out> the type the websocket outputs back to remote clients
   */
  public static class Accepted<In, Out> {
    private final Flow<In, Out, ?> flow;
    private final Optional<String> subprotocol;
    private final List<Map.Entry<String, String>> headers;
    private final List<Http.Cookie> cookies;
    private final Http.Session session;

    public Accepted(Flow<In, Out, ?> flow, Optional<String> subprotocol) {
      this(flow, subprotocol, Collections.emptyList(), Collections.emptyList());
    }

    public Accepted(
        Flow<In, Out, ?> flow,
        Optional<String> subprotocol,
        List<Map.Entry<String, String>> headers,
        List<Http.Cookie> cookies) {
      this(flow, subprotocol, headers, cookies, null);
    }

    public Accepted(
        Flow<In, Out, ?> flow,
        Optional<String> subprotocol,
        List<Map.Entry<String, String>> headers,
        List<Http.Cookie> cookies,
        Http.Session session) {
      this.flow = flow;
      this.subprotocol = subprotocol;
      this.headers = Collections.unmodifiableList(new ArrayList<>(headers));
      this.cookies = Collections.unmodifiableList(new ArrayList<>(cookies));
      this.session = session;
    }

    public Accepted(Flow<In, Out, ?> flow, String subprotocol) {
      this(flow, Optional.of(subprotocol));
    }

    public Accepted(Flow<In, Out, ?> flow) {
      this(flow, Optional.empty());
    }

    public Flow<In, Out, ?> flow() {
      return flow;
    }

    public Optional<String> subprotocol() {
      return subprotocol;
    }

    public List<Map.Entry<String, String>> headers() {
      return headers;
    }

    public List<Http.Cookie> cookies() {
      return cookies;
    }

    public Optional<Http.Session> session() {
      return Optional.ofNullable(session);
    }

    /**
     * Return a copy of this accepted WebSocket with the given handshake response header.
     *
     * @param name the header name
     * @param value the header value
     * @return the transformed copy
     */
    public Accepted<In, Out> withHeader(String name, String value) {
      List<Map.Entry<String, String>> newHeaders = new ArrayList<>(headers);
      newHeaders.add(new AbstractMap.SimpleImmutableEntry<>(name, value));
      return new Accepted<>(flow, subprotocol, newHeaders, cookies, session);
    }

    /**
     * Return a copy of this accepted WebSocket with the given handshake response headers.
     *
     * <p>The headers are processed in pairs, so nameValues(0) is the first header's name, and
     * nameValues(1) is the first header's value, nameValues(2) is second header's name, and so on.
     *
     * @param nameValues the array of names and values.
     * @return the transformed copy
     */
    public Accepted<In, Out> withHeaders(String... nameValues) {
      if (nameValues.length % 2 != 0) {
        throw new IllegalArgumentException(
            "Headers must be supplied as alternating name and value strings");
      }

      List<Map.Entry<String, String>> newHeaders = new ArrayList<>(headers);
      for (int i = 0; i < nameValues.length; i += 2) {
        newHeaders.add(new AbstractMap.SimpleImmutableEntry<>(nameValues[i], nameValues[i + 1]));
      }
      return new Accepted<>(flow, subprotocol, newHeaders, cookies, session);
    }

    /**
     * Discard a handshake response header.
     *
     * @param name the header name
     * @return the transformed copy
     */
    public Accepted<In, Out> withoutHeader(String name) {
      String lowerName = name.toLowerCase(Locale.ROOT);
      List<Map.Entry<String, String>> newHeaders =
          headers.stream()
              .filter(header -> !header.getKey().toLowerCase(Locale.ROOT).equals(lowerName))
              .collect(Collectors.toList());
      return new Accepted<>(flow, subprotocol, newHeaders, cookies, session);
    }

    /**
     * Returns a copy of this accepted WebSocket with the given cookies.
     *
     * @param newCookies the cookies to add to the handshake response.
     * @return the transformed copy.
     */
    public Accepted<In, Out> withCookies(Http.Cookie... newCookies) {
      List<Http.Cookie> finalCookies =
          Stream.concat(
                  cookies.stream()
                      .filter(
                          cookie -> {
                            for (Http.Cookie newCookie : newCookies) {
                              if (cookie.name().equals(newCookie.name())) return false;
                            }
                            return true;
                          }),
                  Stream.of(newCookies))
              .collect(Collectors.toList());
      return new Accepted<>(flow, subprotocol, headers, finalCookies, session);
    }

    /**
     * Discard a cookie on the default path ("/") with no domain and that's not secure.
     *
     * @param name The name of the cookie to discard, must not be null
     */
    public Accepted<In, Out> discardingCookie(String name) {
      return discardingCookie(name, "/");
    }

    /**
     * Discard a cookie on the given path with no domain and not that's secure.
     *
     * @param name The name of the cookie to discard, must not be null
     * @param path The path of the cookie to discard, may be null
     */
    public Accepted<In, Out> discardingCookie(String name, String path) {
      return discardingCookie(name, path, null);
    }

    /**
     * Discard a cookie on the given path and domain that's not secure.
     *
     * @param name The name of the cookie to discard, must not be null
     * @param path The path of the cookie te discard, may be null
     * @param domain The domain of the cookie to discard, may be null
     */
    public Accepted<In, Out> discardingCookie(String name, String path, String domain) {
      return withCookies(new Http.Cookie(name, "", 0, path, domain, false, true, null, false));
    }

    /**
     * @param request Current request
     * @return The session carried by this WebSocket upgrade response. Reads the given request's
     *     session if this response does not modify the session.
     */
    public Http.Session session(Http.RequestHeader request) {
      if (session != null) {
        return session;
      } else {
        return request.session();
      }
    }

    /**
     * Sets a new session for this WebSocket upgrade response, discarding the existing session.
     *
     * @param session the session to set with this WebSocket upgrade response
     * @return the transformed copy
     */
    public Accepted<In, Out> withSession(Http.Session session) {
      return new Accepted<>(flow, subprotocol, headers, cookies, session);
    }

    /**
     * Sets a new session for this WebSocket upgrade response, discarding the existing session.
     *
     * @param session the session to set with this WebSocket upgrade response
     * @return the transformed copy
     */
    public Accepted<In, Out> withSession(Map<String, String> session) {
      return withSession(new Http.Session(session));
    }

    /**
     * Discards the existing session for this WebSocket upgrade response.
     *
     * @return the transformed copy
     */
    public Accepted<In, Out> withNewSession() {
      return withSession(Collections.emptyMap());
    }

    /**
     * Adds values to this WebSocket upgrade response's session.
     *
     * @param values A map with values to add to this response's session
     * @return the transformed copy
     */
    public Accepted<In, Out> addingToSession(
        Http.RequestHeader request, Map<String, String> values) {
      return withSession(session(request).adding(values));
    }

    /**
     * Adds the given key and value to this WebSocket upgrade response's session.
     *
     * @param key The key to add to this response's session
     * @param value The value to add to this response's session
     * @return the transformed copy
     */
    public Accepted<In, Out> addingToSession(Http.RequestHeader request, String key, String value) {
      Map<String, String> newValues = new HashMap<>(1);
      newValues.put(key, value);
      return addingToSession(request, newValues);
    }

    /**
     * Removes values from this WebSocket upgrade response's session.
     *
     * @param keys Keys to remove from session
     * @return the transformed copy
     */
    public Accepted<In, Out> removingFromSession(Http.RequestHeader request, String... keys) {
      return withSession(session(request).removing(keys));
    }
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

    public MappedWebSocketAcceptor(
        PartialFunction<Message, F.Either<In, Message>> inMapper,
        Function<Out, Message> outMapper) {
      this.inMapper = inMapper;
      this.outMapper = outMapper;
    }

    /**
     * Accept a WebSocket.
     *
     * @param f A function that takes the request header, and returns a future of either the result
     *     to reject the WebSocket connection with, or a flow to handle the WebSocket messages.
     * @return The WebSocket handler.
     */
    public WebSocket acceptOrResult(
        Function<Http.RequestHeader, CompletionStage<F.Either<Result, Flow<In, Out, ?>>>> f) {
      return WebSocket.acceptOrResult(inMapper, f, outMapper);
    }

    /**
     * Accept a WebSocket with handshake metadata.
     *
     * @param f A function that takes the request header, and returns a future of either the result
     *     to reject the WebSocket connection with, or an accepted WebSocket with its flow and
     *     handshake metadata.
     * @return The WebSocket handler.
     */
    public WebSocket acceptOrResultWithOptions(
        Function<Http.RequestHeader, CompletionStage<F.Either<Result, Accepted<In, Out>>>> f) {
      return WebSocket.acceptOrResultWithOptions(inMapper, f, outMapper);
    }

    /**
     * Accept a WebSocket.
     *
     * @param f A function that takes the request header, and returns a flow to handle the WebSocket
     *     messages.
     * @return The WebSocket handler.
     */
    public WebSocket accept(Function<Http.RequestHeader, Flow<In, Out, ?>> f) {
      return acceptOrResult(
          request -> CompletableFuture.completedFuture(F.Either.Right(f.apply(request))));
    }

    /**
     * Accept a WebSocket with handshake metadata.
     *
     * @param f A function that takes the request header, and returns an accepted WebSocket with its
     *     flow and handshake metadata.
     * @return The WebSocket handler.
     */
    public WebSocket acceptWithOptions(Function<Http.RequestHeader, Accepted<In, Out>> f) {
      return acceptOrResultWithOptions(
          request -> CompletableFuture.completedFuture(F.Either.Right(f.apply(request))));
    }
  }

  /**
   * Helper to create handlers for WebSockets.
   *
   * @param inMapper Function to map input messages. If it produces left, the message will be passed
   *     to the WebSocket flow, if it produces right, the message will be sent back out to the
   *     client - this can be used to send errors directly to the client.
   * @param f The function to handle the WebSocket.
   * @param outMapper Function to map output messages.
   * @return The WebSocket handler.
   */
  private static <In, Out> WebSocket acceptOrResult(
      PartialFunction<Message, F.Either<In, Message>> inMapper,
      Function<Http.RequestHeader, CompletionStage<F.Either<Result, Flow<In, Out, ?>>>> f,
      Function<Out, Message> outMapper) {
    return new WebSocket() {
      @Override
      public CompletionStage<F.Either<Result, Flow<Message, Message, ?>>> apply(
          Http.RequestHeader request) {
        return f.apply(request)
            .thenApply(
                resultOrFlow -> {
                  if (resultOrFlow.left.isPresent()) {
                    return F.Either.Left(resultOrFlow.left.get());
                  } else {
                    Flow<Message, Message, ?> flow =
                        PekkoStreams.bypassWith(
                            Flow.<Message>create().collect(inMapper),
                            play.api.libs.streams.PekkoStreams.onlyFirstCanFinishMerge(2),
                            resultOrFlow.right.get().map(outMapper::apply));
                    return F.Either.Right(flow);
                  }
                });
      }
    };
  }

  private static <In, Out> WebSocket acceptOrResultWithOptions(
      PartialFunction<Message, F.Either<In, Message>> inMapper,
      Function<Http.RequestHeader, CompletionStage<F.Either<Result, Accepted<In, Out>>>> f,
      Function<Out, Message> outMapper) {
    return new WebSocket() {
      @Override
      public CompletionStage<F.Either<Result, Flow<Message, Message, ?>>> apply(
          Http.RequestHeader request) {
        return applyWithOptions(request)
            .thenApply(
                resultOrAccepted -> {
                  if (resultOrAccepted.left.isPresent()) {
                    return F.Either.Left(resultOrAccepted.left.get());
                  } else {
                    return F.Either.Right(resultOrAccepted.right.get().flow());
                  }
                });
      }

      @Override
      public CompletionStage<F.Either<Result, Accepted<Message, Message>>> applyWithOptions(
          Http.RequestHeader request) {
        return f.apply(request)
            .thenApply(
                resultOrAccepted -> {
                  if (resultOrAccepted.left.isPresent()) {
                    return F.Either.Left(resultOrAccepted.left.get());
                  } else {
                    Accepted<In, Out> accepted = resultOrAccepted.right.get();
                    Flow<Message, Message, ?> flow =
                        PekkoStreams.bypassWith(
                            Flow.<Message>create().collect(inMapper),
                            play.api.libs.streams.PekkoStreams.onlyFirstCanFinishMerge(2),
                            accepted.flow().map(outMapper::apply));
                    return F.Either.Right(
                        new Accepted<>(
                            flow,
                            accepted.subprotocol(),
                            accepted.headers(),
                            accepted.cookies(),
                            accepted.session().orElse(null)));
                  }
                });
      }
    };
  }

  private static Optional<String> firstRequestedSubprotocol(Http.RequestHeader request) {
    return request
        .header("Sec-WebSocket-Protocol")
        .flatMap(
            header -> {
              for (String protocol : header.split(",")) {
                String trimmed = protocol.trim();
                if (!trimmed.isEmpty()) {
                  return Optional.of(trimmed);
                }
              }
              return Optional.empty();
            });
  }
}
