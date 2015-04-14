package javaguide.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.libs.F.*;
import play.libs.ws.*;

import static play.test.Helpers.*;
import static org.junit.Assert.*;

// #test-withserver
public class ServerFunctionalTest extends WithServer {

    @Test
    public void testInServer() {
        int timeout = 5000;
        String url = "http://localhost:" + this.testServer.port() + "/";
        assertEquals(NOT_FOUND, WS.url(url).get().get(timeout).getStatus());
    }

}
// #test-withserver
