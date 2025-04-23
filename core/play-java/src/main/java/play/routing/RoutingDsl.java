/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import net.jodah.typetools.TypeResolver;
import play.BuiltInComponents;
import play.api.mvc.BodyParser;
import play.api.mvc.PathBindable;
import play.api.mvc.PathBindable$;
import play.core.j.JavaContextComponents;
import play.core.routing.HandlerInvokerFactory$;
import play.libs.Scala;
import play.mvc.Http;
import play.mvc.Result;
import scala.reflect.ClassTag$;

/**
 * A DSL for building a router.
 *
 * <p>This DSL matches requests based on method and a path pattern, and is able to extract up to
 * three parameters out of the path pattern to pass into lambdas.
 *
 * <p>The passed in lambdas may optionally declare the types of the input parameters. If they don't,
 * the JVM will infer a type of Object, but the parameters themselves are passed in as Strings.
 * Supported types are java.lang.Integer, java.lang.Long, java.lang.Float, java.lang.Double,
 * java.lang.Boolean, and any class that extends play.mvc.PathBindable. The router will attempt to
 * decode parameters using a PathBindable for each of those types, if it fails it will return a 400
 * error.
 *
 * <p>Example usage:
 *
 * <pre>
 * import javax.inject.*;
 * import play.mvc.*;
 * import play.routing.*;
 * import play.libs.json.*;
 * import play.api.routing.Router;
 *
 * public class MyRouterBuilder extends Controller {
 *
 *   private final RoutingDsl routingDsl;
 *
 *   \@Inject
 *   public MyRouterBuilder(RoutingDsl routingDsl) {
 *     this.routingDsl = routingDsl;
 *   }
 *
 *   public Router getRouter() {
 *     return this.routingDsl
 *
 *       .GET("/hello/:to").routingTo((req, to) -&gt; ok("Hello " + to))
 *
 *       .POST("/api/items/:id").routingAsync((Http.Request req, Integer id) -&gt; {
 *         return Items.save(id,
 *           Json.fromJson(req.body().asJson(), Item.class)
 *         ).map(result -&gt; ok("Saved item with id " + id));
 *       })
 *
 *       .build();
 *   }
 * }
 * </pre>
 *
 * The path pattern supports three different types of parameters, path segment parameters, prefixed
 * with :, full path parameters, prefixed with *, and regular expression parameters, prefixed with $
 * and post fixed with a regular expression in angled braces.
 */
public class RoutingDsl {

  private final BodyParser<Http.RequestBody> bodyParser;

  final List<Route> routes = new ArrayList<>();

  @Inject
  public RoutingDsl(play.mvc.BodyParser.Default bodyParser) {
    this.bodyParser = HandlerInvokerFactory$.MODULE$.javaBodyParserToScala(bodyParser);
  }

  /**
   * @deprecated Deprecated as of 2.8.0. Use constructor without JavaContextComponents
   */
  @Deprecated
  public RoutingDsl(
      play.mvc.BodyParser.Default bodyParser, JavaContextComponents contextComponents) {
    this(bodyParser);
  }

  public static RoutingDsl fromComponents(BuiltInComponents components) {
    return new RoutingDsl(components.defaultBodyParser());
  }

  /**
   * Create a GET route for the given path pattern.
   *
   * @param pathPattern The path pattern.
   * @return A GET route matcher.
   */
  public PathPatternMatcher GET(String pathPattern) {
    return new PathPatternMatcher("GET", pathPattern);
  }

  /**
   * Create a HEAD route for the given path pattern.
   *
   * @param pathPattern The path pattern.
   * @return A HEAD route matcher.
   */
  public PathPatternMatcher HEAD(String pathPattern) {
    return new PathPatternMatcher("HEAD", pathPattern);
  }

  /**
   * Create a POST route for the given path pattern.
   *
   * @param pathPattern The path pattern.
   * @return A POST route matcher.
   */
  public PathPatternMatcher POST(String pathPattern) {
    return new PathPatternMatcher("POST", pathPattern);
  }

  /**
   * Create a PUT route for the given path pattern.
   *
   * @param pathPattern The path pattern.
   * @return A PUT route matcher.
   */
  public PathPatternMatcher PUT(String pathPattern) {
    return new PathPatternMatcher("PUT", pathPattern);
  }

  /**
   * Create a DELETE route for the given path pattern.
   *
   * @param pathPattern The path pattern.
   * @return A DELETE route matcher.
   */
  public PathPatternMatcher DELETE(String pathPattern) {
    return new PathPatternMatcher("DELETE", pathPattern);
  }

  /**
   * Create a PATCH route for the given path pattern.
   *
   * @param pathPattern The path pattern.
   * @return A PATCH route matcher.
   */
  public PathPatternMatcher PATCH(String pathPattern) {
    return new PathPatternMatcher("PATCH", pathPattern);
  }

  /**
   * Create a OPTIONS route for the given path pattern.
   *
   * @param pathPattern The path pattern.
   * @return A OPTIONS route matcher.
   */
  public PathPatternMatcher OPTIONS(String pathPattern) {
    return new PathPatternMatcher("OPTIONS", pathPattern);
  }

  /**
   * Create a route for the given method and path pattern.
   *
   * @param method The method;
   * @param pathPattern The path pattern.
   * @return A route matcher.
   */
  public PathPatternMatcher match(String method, String pathPattern) {
    return new PathPatternMatcher(method, pathPattern);
  }

  /**
   * Build the router.
   *
   * @return The built router.
   */
  public play.routing.Router build() {
    return new RouterBuilderHelper(this.bodyParser).build(this);
  }

  private RoutingDsl with(
      String method, String pathPattern, int arity, Object action, Class<?> actionFunction) {

    // Parse the pattern
    Matcher matcher = paramExtractor.matcher(pathPattern);
    List<MatchResult> matches =
        StreamSupport.stream(
                new Spliterators.AbstractSpliterator<MatchResult>(arity, 0) {
                  public boolean tryAdvance(Consumer<? super MatchResult> action) {
                    if (matcher.find()) {
                      action.accept(matcher.toMatchResult());
                      return true;
                    } else {
                      return false;
                    }
                  }
                },
                false)
            .collect(Collectors.toList());

    if (matches.size() != arity) {
      throw new IllegalArgumentException(
          "Path contains "
              + matches.size()
              + " params but function of arity "
              + arity
              + " was passed");
    }

    StringBuilder sb = new StringBuilder();
    List<RouteParam> params = new ArrayList<>(arity);
    Iterator<Class<?>> argumentTypes =
        Arrays.asList(TypeResolver.resolveRawArguments(actionFunction, action.getClass()))
            .iterator();

    int start = 0;
    for (MatchResult result : matches) {
      sb.append(Pattern.quote(pathPattern.substring(start, result.start())));
      String type = result.group(1);
      String name = result.group(2);
      PathBindable<?> pathBindable = pathBindableFor(argumentTypes.next());
      switch (type) {
        case ":":
          sb.append("([^/]+)");
          params.add(new RouteParam(name, true, pathBindable));
          break;
        case "*":
          sb.append("(.*)");
          params.add(new RouteParam(name, false, pathBindable));
          break;
        default:
          sb.append("(").append(result.group(3)).append(")");
          params.add(new RouteParam(name, false, pathBindable));
          break;
      }
      start = result.end();
    }
    sb.append(Pattern.quote(pathPattern.substring(start)));

    Pattern regex = Pattern.compile(sb.toString());

    Method actionMethod = null;
    for (Method m : actionFunction.getMethods()) {
      // Here I assume that we are always passing a `actionFunction` type that:
      // 1) defines exactly one abstract method, and
      // 2) the abstract method is the method that we want to invoke.
      // This works fine with the current implementation of `PathPatternMatcher`, but I wouldn't be
      // surprised if it breaks in the future, which is why this comment exists.
      // Also, the former implementation (which was checking for the first non default method), was
      // not working when using a `java.util.function.Function` type (Function.identity was being
      // returned, instead of Function.apply).
      if (Modifier.isAbstract(m.getModifiers())) {
        actionMethod = m;
      }
    }

    routes.add(new Route(method, regex, params, action, actionMethod));

    return this;
  }

  private PathBindable<?> pathBindableFor(Class<?> clazz) {
    PathBindable<?> builtIn = Scala.orNull(PathBindable$.MODULE$.pathBindableRegister().get(clazz));
    if (builtIn != null) {
      return builtIn;
    } else if (play.mvc.PathBindable.class.isAssignableFrom(clazz)) {
      return javaPathBindableFor(clazz);
    } else if (clazz.equals(Object.class)) {
      // Special case for object, treat as a string
      return PathBindable.bindableString$.MODULE$;
    } else {
      throw new IllegalArgumentException("Don't know how to bind argument of type " + clazz);
    }
  }

  private static <A extends play.mvc.PathBindable<A>> PathBindable<?> javaPathBindableFor(
      Class<?> clazz) {
    return PathBindable$.MODULE$.<A>javaPathBindable(ClassTag$.MODULE$.apply(clazz));
  }

  static class Route {
    final String method;
    final Pattern pathPattern;
    final List<RouteParam> params;
    final Object action;
    final Method actionMethod;

    Route(
        String method,
        Pattern pathPattern,
        List<RouteParam> params,
        Object action,
        Method actionMethod) {
      this.method = method;
      this.pathPattern = pathPattern;
      this.params = params;
      this.action = action;
      this.actionMethod = actionMethod;
    }
  }

  static class RouteParam {
    final String name;
    final Boolean decode;
    final PathBindable<?> pathBindable;

    RouteParam(String name, Boolean decode, PathBindable<?> pathBindable) {
      this.name = name;
      this.decode = decode;
      this.pathBindable = pathBindable;
    }
  }

  private static final Pattern paramExtractor =
      Pattern.compile(
          "([:*$])(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)(?:<(.*)>)?");

  /** A matcher for routes. */
  public class PathPatternMatcher {

    public PathPatternMatcher(String method, String pathPattern) {
      this.method = method;
      this.pathPattern = pathPattern;
    }

    private final String method;
    private final String pathPattern;

    /**
     * Route with the request and no parameters.
     *
     * @param action the action to execute
     * @return this router builder.
     */
    public RoutingDsl routingTo(RequestFunctions.Params0<Result> action) {
      return build(0, action, RequestFunctions.Params0.class);
    }

    /**
     * Route with the request and a single parameter.
     *
     * @param action the action to execute.
     * @param <P1> the first parameter type.
     * @return this router builder.
     */
    public <P1> RoutingDsl routingTo(RequestFunctions.Params1<P1, Result> action) {
      return build(1, action, RequestFunctions.Params1.class);
    }

    /**
     * Route with the request and two parameter.
     *
     * @param action the action to execute.
     * @param <P1> the first parameter type.
     * @param <P2> the second parameter type.
     * @return this router builder.
     */
    public <P1, P2> RoutingDsl routingTo(RequestFunctions.Params2<P1, P2, Result> action) {
      return build(2, action, RequestFunctions.Params2.class);
    }

    /**
     * Route with the request and three parameter.
     *
     * @param action the action to execute.
     * @param <P1> the first parameter type.
     * @param <P2> the second parameter type.
     * @param <P3> the third parameter type.
     * @return this router builder.
     */
    public <P1, P2, P3> RoutingDsl routingTo(RequestFunctions.Params3<P1, P2, P3, Result> action) {
      return build(3, action, RequestFunctions.Params3.class);
    }

    /**
     * Route async with the request and no parameters.
     *
     * @param action The action to execute.
     * @return This router builder.
     */
    public RoutingDsl routingAsync(
        RequestFunctions.Params0<? extends CompletionStage<Result>> action) {
      return build(0, action, RequestFunctions.Params0.class);
    }

    /**
     * Route async with request and a single parameter.
     *
     * @param <P1> the first type parameter
     * @param action The action to execute.
     * @return This router builder.
     */
    public <P1> RoutingDsl routingAsync(
        RequestFunctions.Params1<P1, ? extends CompletionStage<Result>> action) {
      return build(1, action, RequestFunctions.Params1.class);
    }

    /**
     * Route async with request and two parameters.
     *
     * @param <P1> the first type parameter
     * @param <P2> the second type parameter
     * @param action The action to execute.
     * @return This router builder.
     */
    public <P1, P2> RoutingDsl routingAsync(
        RequestFunctions.Params2<P1, P2, ? extends CompletionStage<Result>> action) {
      return build(2, action, RequestFunctions.Params2.class);
    }

    /**
     * Route async with request and three parameters.
     *
     * @param <A1> the first type parameter
     * @param <A2> the second type parameter
     * @param <A3> the third type parameter
     * @param action The action to execute.
     * @return This router builder.
     */
    public <A1, A2, A3> RoutingDsl routingAsync(
        RequestFunctions.Params3<A1, A2, A3, ? extends CompletionStage<Result>> action) {
      return build(3, action, RequestFunctions.Params3.class);
    }

    private <T> RoutingDsl build(int arity, T action, Class<T> actionFunction) {
      return with(method, pathPattern, arity, action, actionFunction);
    }
  }
}
