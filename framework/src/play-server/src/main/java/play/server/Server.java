/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.server;

import play.Mode;
import play.api.routing.Router;
import play.core.j.JavaModeConverter;
import play.core.server.JavaServerHelper;

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
     * Get the port the server is running on.
     */
    public int httpPort() {
        return server.mainAddress().getPort();
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
        return new Server(JavaServerHelper.forRouter(router, JavaModeConverter.asScalaMode(mode), port));
    }

}
