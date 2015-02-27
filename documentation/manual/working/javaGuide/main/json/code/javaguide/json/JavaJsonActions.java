package javaguide.json;
/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import play.mvc.Result;
import play.libs.Json;

import play.mvc.BodyParser;

import javaguide.testhelpers.MockJavaAction;
import play.test.WithApplication;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static play.mvc.Results.ok;
import static play.test.Helpers.*;
import static javaguide.testhelpers.MockJavaActionHelper.call;

public class JavaJsonActions extends WithApplication {

    //#person-class
    // Note: can use getters/setters as well; here we just use public fields directly.
    // if using getters/setters, you can keep keep the fields `protected` or `private`
    public static class Person {
        public String firstName;
        public String lastName;
        public int age;
    }
    //#person-class

    @Test
    public void fromJson() {
        //#from-json
        // parse the JSON as a JsonNode
        JsonNode json = Json.parse("{\"firstName\":\"Foo\", \"lastName\":\"Bar\", \"age\":13}");
        // read the JsonNode as a Person
        Person person = Json.fromJson(json, Person.class);
        //#from-json
        assertThat(person.firstName, equalTo("Foo"));
        assertThat(person.lastName, equalTo("Bar"));
        assertThat(person.age, equalTo(13));
    }

    @Test
    public void toJson() {
        //#to-json
        Person person = new Person();
        person.firstName = "Foo";
        person.lastName = "Bar";
        person.age = 30;
        JsonNode personJson = Json.toJson(person); // {"firstName": "Foo", "lastName": "Bar", "age": 30}
        //#to-json
        assertThat(personJson.get("firstName").asText(), equalTo("Foo"));
        assertThat(personJson.get("lastName").asText(), equalTo("Bar"));
        assertThat(personJson.get("age").asInt(), equalTo(30));
    }

    @Test
    public void customObjectMapper() {
        //#custom-object-mapper
        ObjectMapper mapper = new ObjectMapper()
            // enable features and customize the object mapper here ...
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            // etc.
        Json.setObjectMapper(mapper);
        //#custom-object-mapper
        assertThat(Json.mapper(), equalTo(mapper));
    }

    @Test
    public void requestAsAnyContentAction() {
        assertThat(contentAsString(
            call(new JsonRequestAsJsonAction(), fakeRequest().bodyJson(Json.parse("{\"name\":\"Greg\"}")))
        ), equalTo("Hello Greg"));
    }

    @Test
    public void requestAsJsonAction() {
        assertThat(contentAsString(
            call(new JsonRequestAsJsonAction(), fakeRequest().bodyJson(Json.parse("{\"name\":\"Greg\"}")))
        ), equalTo("Hello Greg"));
    }

    @Test
    public void responseAction() {
        assertThat(contentAsString(
            call(new JsonResponseAction(), fakeRequest())
        ), equalTo("{\"exampleField1\":\"foobar\",\"exampleField2\":\"Hello world!\"}"));
    }

    @Test
    public void responseDaoAction() {
        assertThat(contentAsString(
            call(new JsonResponseDaoAction(), fakeRequest())
        ), equalTo("[{\"firstName\":\"Foo\",\"lastName\":\"Bar\",\"age\":30}]"));
    }

    static class JsonRequestAsAnyContentAction extends MockJavaAction {
        //#json-request-as-anycontent
        public Result sayHello() {
            JsonNode json = request().body().asJson();
            if(json == null) {
                return badRequest("Expecting Json data");
            } else {
                String name = json.findPath("name").textValue();
                if(name == null) {
                    return badRequest("Missing parameter [name]");
                } else {
                    return ok("Hello " + name);
                }
            }
        }
        //#json-request-as-anycontent
    }

    static class JsonRequestAsJsonAction extends MockJavaAction {
        //#json-request-as-json
        @BodyParser.Of(BodyParser.Json.class)
        public Result sayHello() {
            JsonNode json = request().body().asJson();
            String name = json.findPath("name").textValue();
            if(name == null) {
                return badRequest("Missing parameter [name]");
            } else {
                return ok("Hello " + name);
            }
        }
        //#json-request-as-json
    }

    static class JsonResponseAction extends MockJavaAction {
        //#json-response
        public Result sayHello() {
            ObjectNode result = Json.newObject();
            result.put("exampleField1", "foobar");
            result.put("exampleField2", "Hello world!");
            return ok(result);
        }
        //#json-response
    }

    static class JsonResponseDaoAction extends MockJavaAction {
        static class PersonDao {
            public List<Person> findAll() {
                List<Person> people = new ArrayList<Person>();

                Person person = new Person();
                person.firstName = "Foo";
                person.lastName = "Bar";
                person.age = 30;
                people.add(person);

                return people;
            }
        }

        static PersonDao personDao = new PersonDao();

        //#json-response-dao
        public Result getPeople() {
            List<Person> people = personDao.findAll();
            return ok(Json.toJson(people));
        }
        //#json-response-dao
    }
}


