package test;

import controllers.TestController;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import play.test.WithServer;

import static controllers.TestController.Echo;
import static controllers.TestController.ToReturn;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static play.libs.WS.*;
import static play.libs.F.*;

public class WsTest extends WithServer {

    @Before
    public void start() {
        super.start();
    }

    @Test
    public void jsonRequestShouldBeUtf8() throws Exception {
        ObjectNode json = Json.newObject();
        json.put("string", "äüö");
        Echo echo = getEcho(echo().post(json));

        assertThat(echo.headers, hasKey("Content-Type"));
        assertThat(echo.headers.get("Content-Type")[0], equalTo("application/json; charset=utf-8"));
        assertThat(new String(echo.body, "utf-8"), equalTo(Json.stringify(json)));
    }

    @Test
    public void textRequestWithContentTypeSpecifiedShouldBeUtf8() throws Exception {
        Echo echo = getEcho(echo().setContentType("text/html").post("äüö!"));

        assertThat(echo.headers, hasKey("Content-Type"));
        assertThat(echo.headers.get("Content-Type")[0], equalTo("text/html; charset=utf-8"));
        assertThat(new String(echo.body, "utf-8"), equalTo("äüö!"));
    }

    @Test
    public void textRequestWithNoContentTypeSpecifiedShouldBeTextPlainUtf8() throws Exception {
        Echo echo = getEcho(echo().post("äüö!"));

        assertThat(echo.headers, hasKey("Content-Type"));
        assertThat(echo.headers.get("Content-Type")[0], equalTo("text/plain; charset=utf-8"));
        assertThat(new String(echo.body, "utf-8"), equalTo("äüö!"));
    }

    @Test
    public void textRequestWithCharsetSpecifiedShouldUseThatCharset() throws Exception {
        Echo echo = getEcho(echo().setContentType("text/plain; charset=iso8859-1").post("äüö!"));

        assertThat(echo.headers, hasKey("Content-Type"));
        assertThat(echo.headers.get("Content-Type")[0], equalTo("text/plain; charset=iso8859-1"));
        assertThat(new String(echo.body, "iso8859-1"), equalTo("äüö!"));
    }

    @Test
    public void jsonResponseShouldAutomaticallyDetectCharset() throws Exception {
        ObjectNode json = Json.newObject();
        json.put("string", "äüö");
        String js = Json.stringify(json);
        ToReturn toReturn = new ToReturn();

        toReturn.body = js.getBytes("utf-8");
        Response response = slave(toReturn);
        assertThat(Json.stringify(response.asJson()), equalTo(js));

        toReturn.body = js.getBytes("utf-16");
        response = slave(toReturn);
        assertThat(Json.stringify(response.asJson()), equalTo(js));

        toReturn.body = js.getBytes("utf-32");
        response = slave(toReturn);
        assertThat(Json.stringify(response.asJson()), equalTo(js));
    }

    @Test
    public void textResponseShouldHonourSpecifiedCharset() throws Exception {
        ToReturn toReturn = new ToReturn();
        toReturn.body = "äöü!".getBytes("utf-16");
        toReturn.headers.put("Content-Type", "text/plain; charset=utf-16");
        Response response = slave(toReturn);
        assertThat(response.getBody(), equalTo("äöü!"));
    }

    @Test
    public void textResponseShouldDefaultToIso8859() throws Exception {
        // This is in accordance with the HTTP/1.1 spec
        ToReturn toReturn = new ToReturn();
        toReturn.body = "äöü!".getBytes("iso8859-1");
        toReturn.headers.put("Content-Type", "text/plain");
        Response response = slave(toReturn);
        assertThat(response.getBody(), equalTo("äöü!"));
    }

    @Test
    public void nonTextResponseShouldDefaultToUtf8() throws Exception {
        ToReturn toReturn = new ToReturn();
        toReturn.body = "äöü!".getBytes("utf-8");
        toReturn.headers.put("Content-Type", "application/json");
        Response response = slave(toReturn);
        assertThat(response.getBody(), equalTo("äöü!"));
    }

    @Test
    public void noContentTypeResponseShouldDefaultToUtf8() throws Exception {
        ToReturn toReturn = new ToReturn();
        toReturn.body = "äöü!".getBytes("utf-8");
        Response response = slave(toReturn);
        assertThat(response.getBody(), equalTo("äöü!"));
    }

    private Echo getEcho(Promise<Response> response) {
        return Json.fromJson(response.get().asJson(), Echo.class);
    }

    private WSRequestHolder echo() {
        return url("http://localhost:" + port + "/test/echo");
    }

    private Response slave(ToReturn toReturn) {
        return url("http://localhost:" + port + "/test/slave").post(Json.toJson(toReturn)).get();
    }
}
