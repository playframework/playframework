package play.mvc;

import play.api.Application;
import play.api.Play;
import play.api.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import org.junit.Test;
import static org.junit.Assert.*;

public class RequestBuilderTest {

  @Test
  public void testUri_absolute() {
    Request request = new RequestBuilder().uri("https://www.benmccann.com/blog").build();
    assertEquals("https://www.benmccann.com/blog", request.uri());
  }

  @Test
  public void testUri_relative() {
    Request request = new RequestBuilder().uri("/blog").build();
    assertEquals("/blog", request.uri());
  }

  @Test
  public void testUri_asterisk() {
    Request request = new RequestBuilder().method("OPTIONS").uri("*").build();
    assertEquals("*", request.uri());
  }

  @Test
  public void testSecure() {
    assertFalse(new RequestBuilder().uri("http://www.benmccann.com/blog").build().secure());
    assertTrue(new RequestBuilder().uri("https://www.benmccann.com/blog").build().secure());
  }

  @Test
  public void testFlash() {
    Context ctx = new Context(new RequestBuilder().flash("a","1").flash("b","1").flash("b","2"));
    assertEquals("1", ctx.flash().get("a"));
    assertEquals("2", ctx.flash().get("b"));
  }

  @Test
  public void testSession() {
    Application app = new GuiceApplicationBuilder().build();
    Play.start(app);
    Context ctx = new Context(new RequestBuilder().session("a","1").session("b","1").session("b","2"));
    assertEquals("1", ctx.session().get("a"));
    assertEquals("2", ctx.session().get("b"));
    Play.stop(app);
  }

    @Test
    public void testUsername() {
        final Request req1 =
            new RequestBuilder().uri("http://playframework.com/").build();
        final Request req2 = req1.withUsername("user2");

        assertNull(req1.username());
        assertEquals("user2", req2.username());
    }
}
