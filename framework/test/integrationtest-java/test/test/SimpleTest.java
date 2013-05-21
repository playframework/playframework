package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import controllers.routes;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.*;

import play.libs.Json;
import play.mvc.*;
import play.test.*;
import play.data.DynamicForm;
import play.data.validation.ValidationError;
import play.data.validation.Constraints.RequiredValidator;
import play.i18n.Lang;
import play.libs.F;
import play.libs.F.*;
import play.libs.WS;

import play.api.mvc.AsyncResult;

import models.JCustomer;
import play.data.Form;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

public class SimpleTest {

    @Test 
    public void simpleCheck() {
        int a = 1 + 1;
        assertThat(a).isEqualTo(2);
    }
   @Test
   public void sessionCookieShouldOverrideOldValue() {   

        running(fakeApplication(), new Runnable() {
          Boolean shouldNotBeCalled = false;
          @Override
          public void run() {
            FakeRequest req = fakeRequest();
            for (int i = 0; i < 5; i++) {
              req = req.withSession("key" + i, "value" + i);
            }
            for (int i = 0; i < 5; i++) {
              if (!req.getWrappedRequest().session().get("key" + i).isDefined()) {
                shouldNotBeCalled = true;
              }
            }
            assertThat(shouldNotBeCalled).isEqualTo(false);
          }
        });
   }

    @Test
    public void renderTemplate() {
        Content html = views.html.index.render("Coco");
        assertThat(contentType(html)).isEqualTo("text/html");
        assertThat(contentAsString(html)).contains("Coco");
    }
  
    @Test
    public void callIndex() {
        Result result = callAction(controllers.routes.ref.Application.index("Kiki"));   
        assertThat(status(result)).isEqualTo(OK);
        assertThat(contentType(result)).isEqualTo("text/html");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(contentAsString(result)).contains("Hello Kiki");
    }
    
    @Test
    public void badRoute() {
        Result result = routeAndCall(fakeRequest(GET, "/xx/Kiki"));
        assertThat(result).isNull();
    }
    
    @Test
    public void routeIndex() {
        Result result = routeAndCall(fakeRequest(GET, "/Kiki"));
        assertThat(status(result)).isEqualTo(OK);
        assertThat(contentType(result)).isEqualTo("text/html");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(contentAsString(result)).contains("Hello Kiki");
    }
    
    @Test
    public void inApp() {  
        running(fakeApplication(), new Runnable() {
            public void run() {
                Result result = routeAndCall(fakeRequest(GET, "/key"));
                assertThat(status(result)).isEqualTo(OK);
                assertThat(contentType(result)).isEqualTo("text/plain");
                assertThat(charset(result)).isEqualTo("utf-8");
                assertThat(contentAsString(result)).contains("secret");
            }
        });
    }
    
    @Test
    public void inServer() {
        running(testServer(3333), HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
               browser.goTo("http://localhost:3333"); 
               assertThat(browser.$("#title").getTexts().get(0)).isEqualTo("Hello Guest");
               browser.$("a").click();
               assertThat(browser.url()).isEqualTo("http://localhost:3333/Coco");
               assertThat(browser.$("#title", 0).getText()).isEqualTo("Hello Coco");
            }
        });
    }
    
    @Test
    public void errorsAsJson() {
        running(fakeApplication(), new Runnable() {
            @Override
            public void run() {
                Lang lang = new Lang(new play.api.i18n.Lang("en", ""));
                
                Map<String, List<ValidationError>> errors = new HashMap<String, List<ValidationError>>();
                List<ValidationError> error = new ArrayList<ValidationError>();
                error.add(new ValidationError("foo", RequiredValidator.message, new ArrayList<Object>()));
                errors.put("foo", error);
                
                DynamicForm form = new DynamicForm(new HashMap<String, String>(), errors, F.None());
                
                JsonNode jsonErrors = form.errorsAsJson(lang);
                assertThat(jsonErrors.findPath("foo").iterator().next().asText()).isEqualTo(play.i18n.Messages.get(lang, RequiredValidator.message));
            }
        });
    }

    /**
     * Checks that we can build fake request with a json body.
     * In this test, we use the default method (POST).
     */
    @Test
    public void withJsonBody() {
        running(fakeApplication(), new Runnable() {
            @Override
            public void run() {
                Map map = new HashMap();
                map.put("key1", "val1");
                map.put("key2", 2);
                map.put("key3", true);
                JsonNode node = Json.toJson(map);
                Result result = routeAndCall(fakeRequest("POST", "/json").withJsonBody(node));
                assertThat(status(result)).isEqualTo(OK);
                assertThat(contentType(result)).isEqualTo("application/json");
                JsonNode node2 = Json.parse(contentAsString(result));
                assertThat(node2.get("key1").asText()).isEqualTo("val1");
                assertThat(node2.get("key2").asInt()).isEqualTo(2);
                assertThat(node2.get("key3").asBoolean()).isTrue();
            }
        });
    }

    /**
     * Checks that we can build fake request with a json body.
     * In this test we specify the method to use (DELETE)
     */
    @Test
    public void withJsonBodyAndSpecifyMethod() {
        running(fakeApplication(), new Runnable() {
            @Override
            public void run() {
                Map map = new HashMap();
                map.put("key1", "val1");
                map.put("key2", 2);
                map.put("key3", true);
                JsonNode node = Json.toJson(map);
                Result result = callAction(routes.ref.Application.getIdenticalJson(),
                        fakeRequest().withJsonBody(node, "DELETE"));
                assertThat(status(result)).isEqualTo(OK);
                assertThat(contentType(result)).isEqualTo("application/json");
                JsonNode node2 = Json.parse(contentAsString(result));
                assertThat(node2.get("key1").asText()).isEqualTo("val1");
                assertThat(node2.get("key2").asInt()).isEqualTo(2);
                assertThat(node2.get("key3").asBoolean()).isTrue();
            }
        });
    }

    @Test
    public void asyncResult() {

        running(fakeApplication(), new Runnable() {
            @Override
            public void run() {
                Result result = route(fakeRequest(
                        GET, "/async"));
                assertThat(status(result)).isEqualTo(OK);
                assertThat(charset(result)).isEqualTo("utf-8");
                assertThat(contentAsString(result)).isEqualTo("success");
                assertThat(contentType(result)).isEqualTo("text/plain");
                assertThat(header("header_test", result)).isEqualTo(
                        "header_val");
                assertThat(session(result).get("session_test")).isEqualTo(
                        "session_val");
                assertThat(cookie("cookie_test", result).value()).isEqualTo(
                        "cookie_val");
                assertThat(flash(result).get("flash_test")).isEqualTo(
                        "flash_val");
            }
        });
    }

    @Test
    public void nestedContraints() {
        Form<JCustomer> customerForm = new Form<JCustomer>(JCustomer.class);
        // email constraints
        assertThat(customerForm.field("email").constraints().size()).as(
                "field(\"email\").constraints().size()").isEqualTo(2);
        assertThat(customerForm.field("email").constraints().get(0)._1).as(
                "field(\"email\").constraints(0)")
                .isEqualTo("constraint.email");
        assertThat(customerForm.field("email").constraints().get(1)._1).as(
                "field(\"email\").constraints(1)").isEqualTo(
                "constraint.required");
        // orders[0].date constraints
        assertThat(customerForm.field("orders[0].date").constraints().size())
                .as("field(\"orders[0].date\").constraints().size()")
                .isEqualTo(1);
        assertThat(customerForm.field("orders[0].date").constraints().get(0)._1)
                .as("field(\"orders[0].date\").constraints(0)").isEqualTo(
                        "constraint.required");
        // orders[0].date format
        assertThat(customerForm.field("orders[0].date").format()._1).as(
                "field(\"orders[0].date\").format()._1").isEqualTo(
                "format.date");
        assertThat(customerForm.field("orders[0].date").format()._2.toString())
                .as("field(\"orders[0].date\").format()._2").isEqualTo(
                        "[yyyy-MM-dd]");
        // orders[0].items[0].qty constraints
        assertThat(
                customerForm.field("orders[0].items[0].qty").constraints()
                        .size()).as(
                "field(\"orders[0].items[0].qty\").constraints().size()")
                .isEqualTo(2);
        assertThat(
                customerForm.field("orders[0].items[0].qty").constraints()
                        .get(0)._1).as(
                "field(\"orders[0].items[0].qty\").constraints(0)").isEqualTo(
                "constraint.min");
        assertThat(
                customerForm.field("orders[0].items[0].qty").constraints()
                        .get(0)._2.toString()).as(
                "field(\"orders[0].items[0].qty\").constraints(0)._2")
                .isEqualTo("[1]");
        assertThat(
                customerForm.field("orders[0].items[0].qty").constraints()
                        .get(1)._1).as(
                "field(\"orders[0].items[0].qty\").constraints(1)").isEqualTo(
                "constraint.required");
        // orders[0].items[0].productCode constraints
        assertThat(
                customerForm.field("orders[0].items[0].productCode")
                        .constraints().size())
                .as("field(\"orders[0].items[0].productCode\").constraints().size()")
                .isEqualTo(2);
        assertThat(
                customerForm.field("orders[0].items[0].productCode")
                        .constraints().get(0)._1).as(
                "field(\"orders[0].items[0].productCode\").constraints(0)")
                .isEqualTo("constraint.pattern");
        assertThat(
                customerForm.field("orders[0].items[0].productCode")
                        .constraints().get(0)._2.size()).as(
                "field(\"orders[0].items[0].productCode\").constraints(0)")
                .isEqualTo(1);
        assertThat(
                customerForm.field("orders[0].items[0].productCode")
                        .constraints().get(0)._2.get(0)).as(
                "field(\"orders[0].items[0].productCode\").constraints(0)")
                .isEqualTo("[A-Z]{4}-[0-9]{3,}");
        assertThat(
                customerForm.field("orders[0].items[0].productCode")
                        .constraints().get(1)._1).as(
                "field(\"orders[0].items[0].productCode\").constraints(1)")
                .isEqualTo("constraint.required");
        // orders[0].items[0].deliveryDate constraints
        assertThat(
                customerForm.field("orders[0].items[0].deliveryDate")
                        .constraints().size())
                .as("field(\"orders[0].items[0].deliveryDate\").constraints().size()")
                .isEqualTo(0);
        // orders[0].items[0].deliveryDate format
        assertThat(
                customerForm.field("orders[0].items[0].deliveryDate").format()._1)
                .as("field(\"orders[0].items[0].deliveryDate\").format()._1")
                .isEqualTo("format.date");
        assertThat(
                customerForm.field("orders[0].items[0].deliveryDate").format()._2
                        .toString()).as(
                "field(\"orders[0].items[0].deliveryDate\").format()._2")
                .isEqualTo("[yyyy-MM-dd]");
    }

    @Test
    public void actionShouldBeExecutedInCorrectThread() {
        running(testServer(3333), new Runnable() {
            public void run() {
                WS.Response response = WS.url("http://localhost:3333/thread").get().get();
                assertThat(response.getBody()).startsWith("play-akka.actor.default-dispatcher-");
            }
        });
    }

}
