/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.server;

import play.Mode;
import play.api.routing.Router;
import play.core.j.JavaModeConverter;
import play.core.server.JavaServerHelper;
import scala.Int;
import scala.Option;

import java.net.InetSocketAddress;

/**
 * A Play server.
 */
public class Server {

    private final play.core.server.Server server;

    public Server(play.core.server.Server server) {
        this.server = server;
    }

    /**
     * Stop the server.
     */
    public void stop() {
        server.stop();
    }

    /**
     * Get the HTTP port the server is running on.
     *
     * throws IllegalStateException if it is not running on the HTTP protocol
     */
    public int httpPort() {
        if (server.httpPort().isDefined()) {
            return Int.unbox(server.httpPort().get());
        } else {
            throw new IllegalStateException("Server has no HTTP port. Try starting it with \"PortConfig.http(<port num>)\"?");
        }
    }

    /**
     * Get the HTTPS port the server is running on.
     *
     * throws IllegalStateException if it is not running on the HTTPS protocol.
     */
    public int httpsPort() {
        if (server.httpsPort().isDefined()) {
            return Int.unbox(server.httpsPort().get());
        } else {
            throw new IllegalStateException("Server has no HTTPS port. Try starting it with \"PortConfig.https(<port num>)\"?");
        }
    }

    /**
     * Get the address the server is running on.
     */
    public InetSocketAddress mainAddress() {
        return server.mainAddress();
    }

    /**
     * Create a server for the given router.
     * <p>
     * The server will be running on a randomly selected ephemeral port, which can be checked using the httpPort
     * property.
     * <p>
     * The server will be running in TEST mode.
     *
     * @param router The router for the server to serve.
     * @return The running server.
     */
    public static Server forRouter(Router router) {
        return forRouter(router, Mode.TEST, 0);
    }

    /**
     * Create a server for the given router.
     * <p>
     * The server will be running on a randomly selected ephemeral port, which can be checked using the httpPort
     * property.
     *
     * @param router The router for the server to serve.
     * @param mode   The mode the server will run on.
     * @return The running server.
     */
    public static Server forRouter(Router router, Mode mode) {
        return forRouter(router, mode, 0);
    }

    /**
     * Create a server for the given router.
     * <p>
     * The server will be running in TEST mode.
     *
     * @param router The router for the server to serve.
     * @param port   The port the server will run on.
     * @return The running server.
     */
    public static Server forRouter(Router router, int port) {
        return forRouter(router, Mode.TEST, port);
    }

    /**
     * Create a server for the given router.
     *
     * @param router The router for the server to serve.
     * @param mode   The mode the server will run on.
     * @param port   The port the server will run on.
     * @return The running server.
     */
    public static Server forRouter(Router router, Mode mode, int port) {
        return forRouter(router, mode, PortConfig.http(port));
    }

    /**
     * Create a server for the given router
     * <p>
     * The server will open to either HTTP or HTTPS traffic, depending on whether PortConfig.http(port) or
     * PortConfig.https(port) is used.
     *
     * @param router The router for the server to serve
     * @param mode The mode the server will run on
     * @param portConfig The port and protocol the server will run on e.g. PortConfig.http(80), PortConfig.https(443)
     * @return The running server
     */
    public static Server forRouter(Router router, Mode mode, PortConfig portConfig) {
        return new Server(
                JavaServerHelper.forRouter(
                        router,
                        JavaModeConverter.asScalaMode(mode),
                        portConfig.maybeHttpPort(),
                        portConfig.maybeSslPort()
                )
        );
    }

    /**
     * Specifies the protocols supported by the server.
     * A choice of HTTPS will force the server to serve only SSL traffic
     **/
    private enum Protocol {
        HTTP,
        HTTPS
    }

    /**
     * Helper class that specifies protocol/port pairs for the server to expose.
     */
    public static class PortConfig {
        private final Server.Protocol protocol;
        private final int port;

        /**
         * Create a PortConfig in HTTPS mode
         * @param port the port on which to serve ssl traffic
         * @return the PortConfig
         */
        public static PortConfig https(int port) {
            return new PortConfig(Server.Protocol.HTTPS, port);
        }

        /**
         * Create a PortConfig in HTTP mode
         * @param port the port on which to serve http traffic
         * @return the PortConfig
         */
        public static PortConfig http(int port) {
            return new PortConfig(Server.Protocol.HTTP, port);
        }

        private PortConfig(Server.Protocol protocol, int port) {
            this.protocol = protocol;
            this.port = port;
        }

        private Option<Integer> maybeHttpPort() {
            return protocol == Protocol.HTTP? Option.apply(port): Option.empty();
        }

        private Option<Integer> maybeSslPort() {
            return protocol == Protocol.HTTPS? Option.apply(port): Option.empty();
        }
    }
}
