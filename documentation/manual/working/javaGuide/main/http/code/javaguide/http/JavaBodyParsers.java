/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http;

import akka.stream.Materializer;
import org.junit.Before;
import org.junit.Test;
import play.http.HttpErrorHandler;
import play.libs.Json;
import play.test.WithApplication;
import javaguide.testhelpers.MockJavaAction;

//#imports
import play.mvc.*;
import play.mvc.Http.*;
//#imports

import javax.inject.Inject;

import static javaguide.testhelpers.MockJavaActionHelper.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaBodyParsers extends WithApplication {

    @Test
    public void accessRequestBody() {
        assertThat(contentAsString(call(new MockJavaAction() {
            //#request-body
            public Result index() {
                RequestBody body = request().body();
                return ok("Got body: " + body.asText());
            }
            //#request-body
        }, fakeRequest().bodyText("foo"))), containsString("foo"));
    }

    @Test
    public void particularBodyParser() {
        assertThat(contentAsString(call(new MockJavaAction() {
                    //#particular-body-parser
                    @BodyParser.Of(BodyParser.Json.class)
                    public Result index() {
                        RequestBody body = request().body();
                        return ok("Got json: " + body.asJson());
                    }
                    //#particular-body-parser
                }, fakeRequest().bodyJson(Json.toJson("foo")))),
                containsString("\"foo\""));
    }

    @Test
    public void defaultParser() {
        assertThat(call(new MockJavaAction() {
                    //#default-parser
                    public Result save() {
                        RequestBody body = request().body();
                        String textBody = body.asText();

                        if(textBody != null) {
                            return ok("Got: " + textBody);
                        } else {
                            return badRequest("Expecting text/plain request body");
                        }
                    }
                    //#default-parser
                }, fakeRequest().bodyJson(Json.toJson("foo"))).status(),
                equalTo(400));
    }

    @Test
    public void maxLength() {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 1100; i++) {
            body.append("1234567890");
        }
        Materializer mat = app.injector().instanceOf(Materializer.class);
        assertThat(callWithStringBody(new MaxLengthAction(), fakeRequest(), body.toString(), mat).status(),
                equalTo(413));
    }

    public static class MaxLengthAction extends MockJavaAction {
        //#max-length
        // Accept only 10KB of data.
        public static class Text10Kb extends BodyParser.Text {
            @Inject
            public Text10Kb(HttpErrorHandler errorHandler) {
                super(10 * 1024, errorHandler);
            }
        }

        @BodyParser.Of(Text10Kb.class)
        public Result index() {
            return ok("Got body: " + request().body().asText());
        }
        //#max-length
    }
}
