import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.MyModel;

import org.codehaus.jackson.JsonNode;
import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.data.DynamicForm;
import play.data.validation.ValidationError;
import play.data.validation.Constraints.RequiredValidator;
import play.i18n.Lang;
import play.libs.F;
import play.libs.F.*;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;


/**
*
* Simple (JUnit) tests that can call all parts of a play app.
* If you are interested in mocking a whole application, see the wiki for more details.
*
*/
public class ApplicationTest {

    @Test 
    public void simpleCheck() {
        int a = 1 + 1;
        assertThat(a).isEqualTo(2);
    }
    
    @Test
    public void renderTemplate() {

        MyModel model = new MyModel();
        model.firstName = "Guillaume";
        model.lastName = "Bort";
        model.age = 30;

        Content html = views.html.index.render(model);
        assertThat(contentType(html)).isEqualTo("text/html");

        // Verify that the model getters and setters are being called
        assertThat(contentAsString(html)).contains("inside MyModel.getFirstName()");
        assertThat(contentAsString(html)).contains("inside MyModel.setLastName()");
    }
  
   
}
