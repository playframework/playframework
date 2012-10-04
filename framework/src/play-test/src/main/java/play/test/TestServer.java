package play.test;

import scala.None$;
import scala.Option;
import scala.Some;

/**
 * A test Netty web server.
 */
public class TestServer extends play.api.test.TestServer {

    /**
     * A test Netty web server.
     *
     * @param port HTTP port to bind on.
     * @param application The FakeApplication to load in this server.
     */
    public TestServer(int port, FakeApplication application) {
        super(port, application.getWrappedApplication(), (Option) None$.MODULE$);
    }

    /**
     * A test Netty web server with HTTPS support
     * @param port HTTP port to bind on
     * @param application The FakeApplication to load in this server
     * @param sslPort HTTPS port to bind on
     */
    public TestServer(int port, FakeApplication application, int sslPort) {
        super(port, application.getWrappedApplication(), new Some(sslPort));
    }

}