/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.routing;

import play.libs.F;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Builder for a router.
 */
public class RouterBuilder {
    
    final List<Route> routes = new ArrayList<>();

    /**
     * Create a GET route for the given path pattern.
     *
     * @param pathPattern The path pattern.
     * @return A GET route matcher.
     */
    public RouteMatcher GET(String pathPattern) {
        return new RouteMatcher("GET", pathPattern);
    }

    /**
     * Create a HEAD route for the given path pattern.
     *
     * @param pathPattern The path pattern.
     * @return A HEAD route matcher.
     */
    public RouteMatcher HEAD(String pathPattern) {
        return new RouteMatcher("HEAD", pathPattern);
    }

    /**
     * Create a POST route for the given path pattern.
     *
     * @param pathPattern The path pattern.
     * @return A POST route matcher.
     */
    public RouteMatcher POST(String pathPattern) {
        return new RouteMatcher("POST", pathPattern);
    }

    /**
     * Create a PUT route for the given path pattern.
     *
     * @param pathPattern The path pattern.
     * @return A PUT route matcher.
     */
    public RouteMatcher PUT(String pathPattern) {
        return new RouteMatcher("PUT", pathPattern);
    }

    /**
     * Create a DELETE route for the given path pattern.
     *
     * @param pathPattern The path pattern.
     * @return A DELETE route matcher.
     */
    public RouteMatcher DELETE(String pathPattern) {
        return new RouteMatcher("DELETE", pathPattern);
    }

    /**
     * Create a PATCH route for the given path pattern.
     *
     * @param pathPattern The path pattern.
     * @return A PATCH route matcher.
     */
    public RouteMatcher PATCH(String pathPattern) {
        return new RouteMatcher("PATCH", pathPattern);
    }

    /**
     * Create a OPTIONS route for the given path pattern.
     *
     * @param pathPattern The path pattern.
     * @return A OPTIONS route matcher.
     */
    public RouteMatcher OPTIONS(String pathPattern) {
        return new RouteMatcher("OPTIONS", pathPattern);
    }

    /**
     * Create a route for the given method and path pattern.
     *
     * @param method The method;
     * @param pathPattern The path pattern.
     * @return A route matcher.
     */
    public RouteMatcher match(String method, String pathPattern) {
        return new RouteMatcher(method, pathPattern);
    }

    /**
     * Build the router.
     *
     * @return The built router.
     */
    public play.api.routing.Router build() {
        return RouterBuilderHelper.build(this);
    }

    private RouterBuilder with(String method, String pathPattern, int arity, F.Function<List<String>, F.Promise<Result>> action) {
        
        // Parse the pattern
        Matcher matcher = paramExtractor.matcher(pathPattern);
        List<MatchResult> matches = StreamSupport.stream(new Spliterators.AbstractSpliterator<MatchResult>(arity, 0) {
            public boolean tryAdvance(Consumer<? super MatchResult> action) {
                if (matcher.find()) {
                    action.accept(matcher.toMatchResult());
                    return true;
                } else {
                    return false;
                }
            }
        }, false).collect(Collectors.toList());

        if (matches.size() != arity) {
            throw new IllegalArgumentException("Path contains " + matches.size() + " params but function of arity " + arity + " was passed");
        }
        
        StringBuilder sb = new StringBuilder();
        List<RouteParam> params = new ArrayList<>(arity);
        int start = 0;
        for (MatchResult result: matches) {
            sb.append(Pattern.quote(pathPattern.substring(start, result.start())));
            String type = result.group(1);
            String name = result.group(2);
            switch (type) {
                case ":":
                    sb.append("([^/]+)");
                    params.add(new RouteParam(name, true));
                    break;
                case "*":
                    sb.append("(.*)");
                    params.add(new RouteParam(name, false));
                    break;
                default:
                    sb.append("(").append(result.group(3)).append(")");
                    params.add(new RouteParam(name, false));
                    break;
            }
            start = result.end();
        }
        sb.append(Pattern.quote(pathPattern.substring(start, pathPattern.length())));
        
        Pattern regex = Pattern.compile(sb.toString());

        routes.add(new Route(method, regex, params, action));
        
        return this;
    }

    private static class Route {
        final String method;
        final Pattern pathPattern;
        final List<RouteParam> params;
        final F.Function<List<String>, F.Promise<Result>> action;

        Route(String method, Pattern pathPattern, List<RouteParam> params, F.Function<List<String>, F.Promise<Result>> action) {
            this.method = method;
            this.pathPattern = pathPattern;
            this.params = params;
            this.action = action;
        }
    }
    
    private static class RouteParam {
        final String name;
        final Boolean decode;

        RouteParam(String name, Boolean decode) {
            this.name = name;
            this.decode = decode;
        }
    }

    private static final Pattern paramExtractor =
            Pattern.compile("([:\\*\\$])(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)(?:<(.*)>)?");

    /**
     * A matcher for routes.
     */
    public class RouteMatcher {

        public RouteMatcher(String method, String pathPattern) {
            this.method = method;
            this.pathPattern = pathPattern;
        }

        private final String method;
        private final String pathPattern;

        /**
         * Route with no parameters.
         *
         * @param action The action to execute.
         * @return This router builder.
         */
        public RouterBuilder routeTo(F.Function0<Result> action) {
            return build(0, params -> F.Promise.pure(action.apply()));
        }

        /**
         * Route with one parameter.
         *
         * @param action The action to execute.
         * @return This router builder.
         */
        public RouterBuilder routeTo(F.Function<String, Result> action) {
            return build(1, params -> F.Promise.pure(action.apply(params.get(0))));
        }

        /**
         * Route with two parameters.
         *
         * @param action The action to execute.
         * @return This router builder.
         */
        public RouterBuilder routeTo(F.Function2<String, String, Result> action) {
            return build(2, params -> F.Promise.pure(action.apply(params.get(0), params.get(1))));
        }

        /**
         * Route with three parameters.
         *
         * @param action The action to execute.
         * @return This router builder.
         */
        public RouterBuilder routeTo(F.Function3<String, String, String, Result> action) {
            return build(3, params -> F.Promise.pure(action.apply(params.get(0), params.get(1), params.get(2))));
        }

        /**
         * Route with no parameters.
         *
         * @param action The action to execute.
         * @return This router builder.
         */
        public RouterBuilder routeAsync(F.Function0<F.Promise<Result>> action) {
            return build(0, params -> action.apply());
        }

        /**
         * Route with one parameter.
         *
         * @param action The action to execute.
         * @return This router builder.
         */
        public RouterBuilder routeAsync(F.Function<String, F.Promise<Result>> action) {
            return build(1, params -> action.apply(params.get(0)));
        }

        /**
         * Route with two parameters.
         *
         * @param action The action to execute.
         * @return This router builder.
         */
        public RouterBuilder routeAsync(F.Function2<String, String, F.Promise<Result>> action) {
            return build(2, params -> action.apply(params.get(0), params.get(1)));
        }

        /**
         * Route with three parameters.
         *
         * @param action The action to execute.
         * @return This router builder.
         */
        public RouterBuilder routeAsync(F.Function3<String, String, String, F.Promise<Result>> action) {
            return build(3, params -> action.apply(params.get(0), params.get(1), params.get(2)));
        }

        private <T> RouterBuilder build(int arity, F.Function<List<String>, F.Promise<Result>> action) {
            return with(method, pathPattern, arity, action);
        }
    }
}
