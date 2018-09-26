/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.server;

import play.Mode;
import play.BuiltInComponents;
import play.routing.Router;
import play.core.j.JavaModeConverter;
import play.core.server.JavaServerHelper;

import java.net.InetSocketAddress;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import scala.compat.java8.OptionConverters;

/**
 * A Play server.
 */
public class Server {

    private final play.core.server.Server server;

    public Server(play.core.server.Server server) {
        this.server = server;
    }

    /**
     * @return the underlying server.
     */
    public play.core.server.Server underlying() {
        return this.server;
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
     * @throws IllegalStateException if it is not running on the HTTP protocol
     * @return the port number.
     */
    public int httpPort() {
        if (server.httpPort().isDefined()) {
            return (Integer)server.httpPort().get();
        } else {
            throw new IllegalStateException("Server has no HTTP port. Try starting it with \"new Server.Builder().http(<port num>)\"?");
        }
    }

    /**
     * Get the HTTPS port the server is running on.
     *
     * @throws IllegalStateException if it is not running on the HTTPS protocol.
     * @return the port number.
     */
    public int httpsPort() {
        if (server.httpsPort().isDefined()) {
            return (Integer)server.httpsPort().get();
        } else {
            throw new IllegalStateException("Server has no HTTPS port. Try starting it with \"new Server.Builder.https(<port num>)\"?");
        }
    }

    /**
     * Get the address the server is running on.
     * @return the address
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
     * @param block The block that creates the router.
     * @return The running server.
     */
    public static Server forRouter(Function<BuiltInComponents, Router> block) {
        return forRouter(Mode.TEST, 0, block);
    }

    /**
     * Create a server for the given router.
     * <p>
     * The server will be running on a randomly selected ephemeral port, which can be checked using the httpPort
     * property.
     * <p>
     * The server will be running in TEST mode.
     *
     * @param mode   The mode the server will run on.
     * @param block The block that creates the router.
     * @return The running server.
     */
    public static Server forRouter(Mode mode, Function<BuiltInComponents, Router> block) {
        return forRouter(mode, 0, block);
    }

    /**
     * Create a server for the given router.
     * <p>
     * The server will be running on a randomly selected ephemeral port, which can be checked using the httpPort
     * property.
     * <p>
     * The server will be running in TEST mode.
     *
     * @param port   The port the server will run on.
     * @param block The block that creates the router.
     * @return The running server.
     */
    public static Server forRouter(int port, Function<BuiltInComponents, Router> block) {
        return forRouter(Mode.TEST, port, block);
    }

    /**
     * Create a server for the router returned by the given block.
     *
     * @param block  The block which creates a router.
     * @param mode   The mode the server will run on.
     * @param port   The port the server will run on.
     *
     * @return The running server.
     */
    public static Server forRouter(Mode mode, int port, Function<BuiltInComponents, Router> block) {
        return new Builder()
                .mode(mode)
                .http(port)
                .build(block);
    }

    /**
     * Specifies the protocols supported by the server.
     **/
    public enum Protocol {
        HTTP,
        HTTPS
    }

    private static class Config {
        private final Map<Protocol, Integer> _ports;
        private final Mode _mode;

        Config(Map<Protocol, Integer> _ports, Mode mode) {
            this._ports = _ports;
            this._mode = mode;
        }

        public Optional<Integer> maybeHttpPort() {
            return Optional.ofNullable(_ports.get(Protocol.HTTP));
        }

        public Optional<Integer> maybeHttpsPort() {
            return Optional.ofNullable(_ports.get(Protocol.HTTPS));
        }

        public Map<Protocol, Integer> ports() {
            return _ports;
        }

        public Mode mode() {
            return _mode;
        }
    }

    /**
     * Configures and builds an embedded server. If not further configured, it will default
     * to serving TEST mode over HTTP on a random available port.
     */
    public static class Builder {
        private Server.Config _config = new Server.Config(new EnumMap<>(Protocol.class), Mode.TEST);

        /**
         * Instruct the server to serve HTTP on a particular port.
         *
         * Passing 0 will make it serve on a random available port.
         *
         * @param port the port on which to serve http traffic
         * @return the builder with port set.
         */
        public Builder http(int port) {
            return _protocol(Protocol.HTTP, port);
        }

        /**
         * Configure the server to serve HTTPS on a particular port.
         *
         * Passing 0 will make it serve on a random available port.
         *
         * @param port the port on which to serve ssl traffic
         * @return the builder with port set.
         */
        public Builder https(int port) {
            return _protocol(Protocol.HTTPS, port);
        }

        /**
         * Set the mode the server should be run on (defaults to TEST)
         *
         * @param mode the Play mode (dev, prod, test)
         * @return the builder with Server.Config set to mode.
         */
        public Builder mode(Mode mode) {
            _config = new Server.Config(_config.ports(), mode);
            return this;
        }

        /**
         * Build the server and begin serving the provided routes as configured.
         *
         * @param router the router to use.
         * @return the actively running server.
         */
        public Server build(final Router router) {
            return build((components) -> router);
        }

        /**
         * Build the server and begin serving the provided routes as configured.
         *
         * @param block the router to use.
         * @return the actively running server.
         */
        public Server build(Function<BuiltInComponents, Router> block) {
            Server.Config config = _buildConfig();
            return new Server(
                    JavaServerHelper.forRouter(
                        JavaModeConverter.asScalaMode(config.mode()),
                        OptionConverters.toScala(config.maybeHttpPort()),
                        OptionConverters.toScala(config.maybeHttpsPort()),
                        block
                    )
            );
        }

        //
        // Private members
        //
        private Server.Config _buildConfig() {
            Builder builder = this;
            if (_config.ports().isEmpty()) {
                builder = this._protocol(Protocol.HTTP, 0);
            }

            return builder._config;
        }

        private Builder _protocol(Protocol protocol, int port) {
            Map<Protocol, Integer> newPorts = new EnumMap<>(Protocol.class);
            newPorts.putAll(_config.ports());
            newPorts.put(protocol, port);

            _config = new Server.Config(newPorts, _config.mode());

            return this;
        }
    }
}
