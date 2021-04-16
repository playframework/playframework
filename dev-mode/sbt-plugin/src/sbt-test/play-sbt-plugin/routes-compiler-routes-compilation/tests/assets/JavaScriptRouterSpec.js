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
    it("should add parameters to the query string", function() {
        var data = jsRoutes.controllers.Application.takeBool(true);
        assert.equal("/take-bool?b=true", data.url);
    });
    it("should add parameters to the query string when they are default", function() {
        var data = jsRoutes.controllers.Application.takeOptionalIntWithDefault(123);
        assert.equal("/take-joptint-d?x=123", data.url);
    });
    it("should add parameters to the query string when they are not default", function() {
        var data = jsRoutes.controllers.Application.takeOptionalIntWithDefault(987);
        assert.equal("/take-joptint-d?x=987", data.url);
    });
    it("should add parameters with custom binding to the query string", function() {
        var data = jsRoutes.controllers.Application.takeOptionalInt(123);
        assert.equal("/take-joptint?x=123", data.url);
    });
    it("should generate an url when parameter with custom binding is not passed", function() {
        var data = jsRoutes.controllers.Application.takeOptionalInt();
        assert.equal("/take-joptint", data.url);
    });
    it("should generate an url when default parameter is not passed", function() {
        var data = jsRoutes.controllers.Application.takeOptionalIntWithDefault();
        assert.equal("/take-joptint-d", data.url);
    });
    it("should generate a url for assets", function() {
        var data = jsRoutes.controllers.Assets.versioned('hello.png');
        assert.equal("/public/hello.png", data.url);
    });
    it("should provide the GET method for assets", function() {
        var data = jsRoutes.controllers.Assets.versioned();
        assert.equal("GET", data.method);
    });
});
