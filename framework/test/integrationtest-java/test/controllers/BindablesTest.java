package controllers;

import models.User;
import org.junit.Test;
import play.mvc.Call;
import play.mvc.Result;
import play.test.WithApplication;
import static play.test.Helpers.*;
import static org.junit.Assert.*;

public class BindablesTest extends WithApplication {
    @Test
    public void testPager() {
        start();
        Pager pager = new Pager();
        pager.index = 10;
        pager.size = 20;
        // Test that the pager is bound/unbound correctly
        Call call = routes.Application.paged(pager);
        assertEquals("/paged?p.index=10&p.size=20", call.url());
        Result result = route(fakeRequest(call.method(), call.url()));
        assertEquals("Pager{index=10, size=20}", contentAsString(result));
    }

    @Test
    public void testUser() {
        start();
        User user = new User();
        user.email = "bob@example.com";
        // Test that the user is bound/unbound correctly
        Call call = routes.Application.user(user);
        assertEquals("/user/bob@example.com", call.url());
        Result result = route(fakeRequest(call.method(), call.url()));
        assertEquals("bob@example.com", contentAsString(result));
    }

}
