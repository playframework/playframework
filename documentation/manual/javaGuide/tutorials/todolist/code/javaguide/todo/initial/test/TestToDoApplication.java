package javaguide.todo.initial.test;

import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.libs.F.*;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

import javaguide.todo.initial.views.html.*;
import javaguide.todo.initial.controllers.*;

public class TestToDoApplication {

	@Test
	public void renderTemplate() {
	  Content html = index.render("Hello");
	  assertThat(contentType(html)).isEqualTo("text/html");
	  assertThat(contentAsString(html)).contains("Hello");
	}

	@Test
	public void callIndex() {
		Result result = Application.index();
		
		assertThat(status(result)).isEqualTo(OK);
		assertThat(contentType(result)).isEqualTo("text/html");
		assertThat(charset(result)).isEqualTo("utf-8");
		assertThat(contentAsString(result)).contains("Your new application is ready.");
	}

}