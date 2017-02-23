/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.server;

import play.Mode;
import play.routing.Router;
import play.core.j.JavaModeConverter;
import play.core.server.JavaServerHelper;
import scala.Int;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
            throw new IllegalStateException("Server has no HTTP port. Try starting it with \"new Server.Builder().http(<port num>)\"?");
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
            throw new IllegalStateException("Server has no HTTPS port. Try starting it with \"new Server.Builder.https(<port num>)\"?");
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
        return new Builder()
                .mode(mode)
                .http(port)
                .build(router);
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
        private Server.Config _config = new Server.Config(new HashMap<>(), Mode.TEST);

        /**
         * Instruct the server to serve HTTP on a particular port.
         *
         * Passing 0 will make it serve on a random available port.
         *
         * @param port the port on which to serve http traffic
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
         */
        public Builder https(int port) {
            return _protocol(Protocol.HTTPS, port);
        }

        /**
         * Set the mode the server should be run on (defaults to TEST)
         */
        public Builder mode(Mode mode) {
            _config = new Server.Config(_config.ports(), mode);
            return this;
        }

        /**
         * Build the server and begin serving the provided routes as configured.
         *
         * @return the actively running server.
         */
        public Server build(Router router) {
            Server.Config config = _buildConfig();
            return new Server(
                    JavaServerHelper.forRouter(
                            router.asScala(),
                            JavaModeConverter.asScalaMode(config.mode()),
                            OptionConverters.toScala(config.maybeHttpPort()),
                            OptionConverters.toScala(config.maybeHttpsPort())
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
            Map<Protocol, Integer> newPorts = new HashMap<>();
            newPorts.putAll(_config.ports());
            newPorts.put(protocol, port);

            _config = new Server.Config(newPorts, _config.mode());

            return this;
        }
    }
}
