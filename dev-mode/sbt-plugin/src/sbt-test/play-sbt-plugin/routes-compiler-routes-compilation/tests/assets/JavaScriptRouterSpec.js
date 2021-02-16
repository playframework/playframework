/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
var assert = require("assert");
var jsRoutes = require("./jsRoutes");

describe("The JavaScript router", function() {
    it("should generate a url", function() {
        var data = jsRoutes.controllers.Application.index();
        assert.equal("/", data.url);
    });
    it("should provide the GET method", function() {
        var data = jsRoutes.controllers.Application.index();
        assert.equal("GET", data.method);
    });
    it("should provide the POST method", function() {
        var data = jsRoutes.controllers.Application.post();
        assert.equal("POST", data.method);
    });
    it("should add parameters to the path", function() {
        var data = jsRoutes.controllers.Application.withParam("foo");
        assert.equal("/with/foo", data.url);
    });
    it("should handle default path param of type String", function() {
        var data = jsRoutes.controllers.Application.withParam("beer");
        assert.equal("/with", data.url);
    });
    it("should handle default path param of type Option[String]", function() {
        var data = jsRoutes.controllers.Application.withParamOption("wine");
        assert.equal("/withOpt", data.url);
    });
    it("should handle path param None of type Option[String]", function() {
        var data = jsRoutes.controllers.Application.withParamOption();
        assert.equal("/withOptNone", data.url);
    });
    it("should handle default path param of type JOptional[String]", function() {
        var data = jsRoutes.controllers.Application.withParamOptional("coffee");
        assert.equal("/withJOpt", data.url);
    });
    it("should handle path param Empty of type JOptional[String]", function() {
        var data = jsRoutes.controllers.Application.withParamOptional();
        assert.equal("/withJOptEmpty", data.url);
    });
    it("should add parameters to the query string", function() {
        var data = jsRoutes.controllers.Application.takeBool(true);
        assert.equal("/take-bool?b=true", data.url);
    });
    it("should generate a url for assets", function() {
        var data = jsRoutes.controllers.Assets.versioned('hello.png');
        assert.equal("/public/hello.png", data.url);
    });
    it("should provide the GET method for assets", function() {
        var data = jsRoutes.controllers.Assets.versioned();
        assert.equal("GET", data.method);
    });
    it("should add parameters to the path where path param is of type Option[String]", function() {
        var data = jsRoutes.controllers.Application.withParamOption("fooOpt");
        assert.equal("/withOpt/fooOpt", data.url);
    });
    it("should add parameters to the path where path param is of type JOptional[String]", function() {
        var data = jsRoutes.controllers.Application.withParamOptional("fooJOpt");
        assert.equal("/withJOpt/fooJOpt", data.url);
    });
    it("should add parameters to the path where path param is of type UUID", function() {
        var data = jsRoutes.controllers.Application.withUUIDParam("7c815c5a-d112-4a69-a6c7-a0fa32361fda");
        assert.equal("/withUUID/7c815c5a-d112-4a69-a6c7-a0fa32361fda", data.url);
    });
    it("should handle default path param of type UUID", function() {
        var data = jsRoutes.controllers.Application.withUUIDParam("11111111-1111-1111-1111-111111111111");
        assert.equal("/withUUID", data.url);
    });
    it("should add parameters to the path where path param is of type Option[UUID]", function() {
        var data = jsRoutes.controllers.Application.withUUIDParamOption("8bd1b515-e269-48cd-8af2-09e093fee383");
        assert.equal("/withUUIDOpt/8bd1b515-e269-48cd-8af2-09e093fee383", data.url);
    });
    it("should handle path param None of type Option[UUID]", function() {
        var data = jsRoutes.controllers.Application.withUUIDParamOption();
        assert.equal("/withUUIDOptNone", data.url);
    });
    it("should handle default path param of type Option[UUID]", function() {
        var data = jsRoutes.controllers.Application.withUUIDParamOption("22222222-2222-2222-2222-222222222222");
        assert.equal("/withUUIDOpt", data.url);
    });
    it("should add parameters to the path where path param is of type JOptional[UUID]", function() {
        var data = jsRoutes.controllers.Application.withUUIDParamOptional("a0801c7f-e5a5-4964-bfae-78fb85e21f72");
        assert.equal("/withUUIDJOpt/a0801c7f-e5a5-4964-bfae-78fb85e21f72", data.url);
    });
    it("should handle path param Empty of type JOptional[UUID]", function() {
        var data = jsRoutes.controllers.Application.withUUIDParamOptional();
        assert.equal("/withUUIDJOptEmpty", data.url);
    });
    it("should handle default path param of type JOptional[UUID]", function() {
        var data = jsRoutes.controllers.Application.withUUIDParamOptional("33333333-3333-3333-3333-333333333333");
        assert.equal("/withUUIDJOpt", data.url);
    });
    it("should add parameters to the path where path param is of type UserId", function() {
        var data = jsRoutes.controllers.Application.user("carl");
        assert.equal("/users/carl", data.url);
    });
    it("should handle default path param of type UserId", function() {
        var data = jsRoutes.controllers.Application.user("123");
        assert.equal("/user", data.url);
    });
    it("should add parameters to the path where path param is of type Option[UserId]", function() {
        var data = jsRoutes.controllers.Application.userOption("john");
        assert.equal("/usersOpt/john", data.url);
    });
    it("should handle path param None of type Option[UserId]", function() {
        var data = jsRoutes.controllers.Application.userOption();
        assert.equal("/usersOptNone", data.url);
    });
    it("should handle default path param of type Option[UserId]", function() {
        var data = jsRoutes.controllers.Application.userOption("abc");
        assert.equal("/usersOpt", data.url);
    });
    it("should add parameters to the path where path param is of type Optional[UserId]", function() {
        var data = jsRoutes.controllers.Application.userOptional("james");
        assert.equal("/usersJOpt/james", data.url);
    });
    it("should handle path param Empty of type JOptional[UserId]", function() {
        var data = jsRoutes.controllers.Application.userOptional();
        assert.equal("/usersJOptEmpty", data.url);
    });
    it("should handle default path param of type JOptional[UserId]", function() {
        var data = jsRoutes.controllers.Application.userOptional("xyz");
        assert.equal("/usersJOpt", data.url);
    });
});
