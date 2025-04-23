/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.json;

import static javaguide.testhelpers.MockJavaActionHelper.call;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javaguide.testhelpers.MockJavaAction;
import org.junit.Test;
import play.core.j.JavaHandlerComponents;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

public class JavaJsonActions extends WithApplication {

  // #person-class
  // Note: can use getters/setters as well; here we just use public fields directly.
  // if using getters/setters, you can keep the fields `protected` or `private`
  public static class Person {
    public String firstName;
    public String lastName;
    public int age;
  }

  // #person-class

  @Test
  public void fromJson() {
    // #from-json
    // parse the JSON as a JsonNode
    JsonNode json = Json.parse("{\"firstName\":\"Foo\", \"lastName\":\"Bar\", \"age\":13}");
    // read the JsonNode as a Person
    Person person = Json.fromJson(json, Person.class);
    // #from-json
    assertThat(person.firstName, equalTo("Foo"));
    assertThat(person.lastName, equalTo("Bar"));
    assertThat(person.age, equalTo(13));
  }

  @Test
  public void toJson() {
    // #to-json
    Person person = new Person();
    person.firstName = "Foo";
    person.lastName = "Bar";
    person.age = 30;
    JsonNode personJson = Json.toJson(person); // {"firstName": "Foo", "lastName": "Bar", "age": 30}
    // #to-json
    assertThat(personJson.get("firstName").asText(), equalTo("Foo"));
    assertThat(personJson.get("lastName").asText(), equalTo("Bar"));
    assertThat(personJson.get("age").asInt(), equalTo(30));
  }

  @Test
  public void requestAsAnyContentAction() {
    assertThat(
        contentAsString(
            call(
                new JsonRequestAsAnyContentAction(instanceOf(JavaHandlerComponents.class)),
                fakeRequest().bodyJson(Json.parse("{\"name\":\"Greg\"}")),
                mat)),
        equalTo("Hello Greg"));
  }

  @Test
  public void requestAsJsonAction() {
    assertThat(
        contentAsString(
            call(
                new JsonRequestAsJsonAction(instanceOf(JavaHandlerComponents.class)),
                fakeRequest().bodyJson(Json.parse("{\"name\":\"Greg\"}")),
                mat)),
        equalTo("Hello Greg"));
  }

  @Test
  public void responseAction() {
    assertThat(
        contentAsString(
            call(
                new JsonResponseAction(instanceOf(JavaHandlerComponents.class)),
                fakeRequest(),
                mat)),
        equalTo("{\"exampleField1\":\"foobar\",\"exampleField2\":\"Hello world!\"}"));
  }

  @Test
  public void responseDaoAction() {
    assertThat(
        contentAsString(
            call(
                new JsonResponseDaoAction(instanceOf(JavaHandlerComponents.class)),
                fakeRequest(),
                mat)),
        equalTo("[{\"firstName\":\"Foo\",\"lastName\":\"Bar\",\"age\":30}]"));
  }

  static class JsonRequestAsAnyContentAction extends MockJavaAction {

    JsonRequestAsAnyContentAction(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #json-request-as-anycontent
    public Result sayHello(Http.Request request) {
      JsonNode json = request.body().asJson();
      if (json == null) {
        return badRequest("Expecting Json data");
      } else {
        String name = json.findPath("name").textValue();
        if (name == null) {
          return badRequest("Missing parameter [name]");
        } else {
          return ok("Hello " + name);
        }
      }
    }
    // #json-request-as-anycontent
  }

  static class JsonRequestAsAnyClazzAction extends MockJavaAction {

    JsonRequestAsAnyClazzAction(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #json-request-as-anyclazz
    public Result sayHello(Http.Request request) {
      Optional<Person> person = request.body().parseJson(Person.class);
      return person.map(p -> ok("Hello, " + p.firstName)).orElse(badRequest("Expecting Json data"));
    }
    // #json-request-as-anyclazz
  }

  static class JsonRequestAsJsonAction extends MockJavaAction {

    JsonRequestAsJsonAction(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #json-request-as-json
    @BodyParser.Of(BodyParser.Json.class)
    public Result sayHello(Http.Request request) {
      JsonNode json = request.body().asJson();
      String name = json.findPath("name").textValue();
      if (name == null) {
        return badRequest("Missing parameter [name]");
      } else {
        return ok("Hello " + name);
      }
    }
    // #json-request-as-json
  }

  static class JsonResponseAction extends MockJavaAction {
    JsonResponseAction(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #json-response
    public Result sayHello() {
      ObjectNode result = Json.newObject();
      result.put("exampleField1", "foobar");
      result.put("exampleField2", "Hello world!");
      return ok(result);
    }
    // #json-response
  }

  static class JsonResponseDaoAction extends MockJavaAction {
    JsonResponseDaoAction(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    static class PersonDao {
      public List<Person> findAll() {
        List<Person> people = new ArrayList<>();

        Person person = new Person();
        person.firstName = "Foo";
        person.lastName = "Bar";
        person.age = 30;
        people.add(person);

        return people;
      }
    }

    static PersonDao personDao = new PersonDao();

    // #json-response-dao
    public Result getPeople() {
      List<Person> people = personDao.findAll();
      return ok(Json.toJson(people));
    }
    // #json-response-dao
  }

  static class JsonStringResponseAction extends MockJavaAction {
    JsonStringResponseAction(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #json-response-string
    public Result sayHello() {
      String jsonString = "{\"exampleField1\": \"foobar\"}";
      return ok(jsonString).as(play.mvc.Http.MimeTypes.JSON);
    }
    // #json-response-string
  }
}
