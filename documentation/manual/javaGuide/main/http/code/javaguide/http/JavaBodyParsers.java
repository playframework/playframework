/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http;

import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import play.test.WithApplication;
import javaguide.testhelpers.MockJavaAction;

//#imports
import play.mvc.*;
import play.mvc.Http.*;
//#imports

import static javaguide.testhelpers.MockJavaAction.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaBodyParsers extends WithApplication {

    @Test
    public void accessRequestBody() {
        assertThat(contentAsString(call(new Controller1(), fakeRequest().withTextBody("foo"))), containsString("foo"));
    }

    public static class Controller1 extends MockJavaAction {
        //#request-body
        public static Result index() {
            RequestBody body = request().body();
            return ok("Got body: " + body);
        }
        //#request-body
    }

    @Test
    public void particularBodyParser() {
        assertThat(contentAsString(call(new Controller2(), fakeRequest().withJsonBody(Json.toJson("foo")))),
                containsString("\"foo\""));
    }

    public static class Controller2 extends MockJavaAction {
        //#particular-body-parser
        @BodyParser.Of(BodyParser.Json.class)
        public static Result index() {
            RequestBody body = request().body();
            return ok("Got json: " + body.asJson());
        }
        //#particular-body-parser
    }

    @Test
    public void defaultParser() {
        assertThat(status(call(new Controller3(), fakeRequest().withJsonBody(Json.toJson("foo")))),
                equalTo(400));
    }

    public static class Controller3 extends MockJavaAction {
        //#default-parser
        public static Result save() {
            RequestBody body = request().body();
            String textBody = body.asText();

            if(textBody != null) {
                return ok("Got: " + textBody);
            } else {
                return badRequest("Expecting text/plain request body");
            }
        }
        //#default-parser
    }

    @Test
    public void maxLength() {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 1100; i++) {
            body.append("1234567890");
        }
        assertThat(status(callWithStringBody(new Controller4(), fakeRequest(), body.toString())),
                equalTo(400));
    }

    public static class Controller4 extends MockJavaAction {
        //#max-length
        // Accept only 10KB of data.
        @BodyParser.Of(value = BodyParser.Text.class, maxLength = 10 * 1024)
        public static Result index() {
            if(request().body().isMaxSizeExceeded()) {
                return badRequest("Too much data!");
            } else {
                return ok("Got body: " + request().body().asText());
            }
        }
        //#max-length
    }

}
