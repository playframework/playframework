/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

var assert = require("assert");
var jsRoutes = require("./jsRoutes");

describe("The JavaScript router for actions taking the Java request", function() {
    it("should generate a url", function() {
        var data = jsRoutes.controllers.JavaRequestController.index();
        assert.equal("/java-request", data.url);
    });
    it("should provide the GET method", function() {
        var data = jsRoutes.controllers.JavaRequestController.index();
        assert.equal("GET", data.method);
    });
    it("should provide the POST method", function() {
        var data = jsRoutes.controllers.JavaRequestController.post();
        assert.equal("POST", data.method);
    });
    it("should add parameters to the path", function() {
        var data = jsRoutes.controllers.JavaRequestController.withParam("foo");
        assert.equal("/java-request/with/foo", data.url);
    });
    it("should add parameters to the query string", function() {
        var data = jsRoutes.controllers.JavaRequestController.takeBool(true);
        assert.equal("/java-request/take-bool?b=true", data.url);
    });
    it("should add complex named parameters to the query string", function() {
        var data = jsRoutes.controllers.JavaRequestController.takeList([1,2,3]);
        var qs = [1,2,3].map(function(i){return 'x=' + i}).join('&');
        assert.equal("/java-request/take-list?" + qs, data.url);
    });
});
