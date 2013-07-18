import org.junit.Test;

import play.mvc.Result;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;


public class FunctionalTest {
    @Test
    public void sendJavaScript() {
        running(fakeApplication(), new Runnable() {
            @Override
            public void run() {
                Result result = callAction(controllers.routes.ref.Application.chatRoomJs("'"));
                assertThat(status(result)).isEqualTo(OK);
                assertThat(contentType(result)).isEqualTo("text/javascript");
            }
        });
    }

    @Test
    public void resistToXSS() {
        running(fakeApplication(), new Runnable() {
            @Override
            public void run() {
                Result result = callAction(controllers.routes.ref.Application.chatRoomJs("'"));
                assertThat(contentAsString(result)).contains("if(data.user == '\\'')");
            }
        });
    }
}
