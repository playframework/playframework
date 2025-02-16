/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

var assert = require("assert");
var jsRoutes = require("./jsRoutes");

describe("The JavaScript router", function() {
    it("should generate an url and provide GET method", function() {
        var data = jsRoutes.controllers.MethodController.get();
        assert.equal("GET", data.method);
        assert.equal("/method/get", data.url);
    });
    it("should generate an url and provide POST method", function() {
        var data = jsRoutes.controllers.MethodController.post();
        assert.equal("POST", data.method);
        assert.equal("/method/post", data.url);
    });
    it("should generate an url and provide PUT method", function() {
        var data = jsRoutes.controllers.MethodController.put();
        assert.equal("PUT", data.method);
        assert.equal("/method/put", data.url);
    });
    it("should generate an url and provide PATCH method", function() {
        var data = jsRoutes.controllers.MethodController.patch();
        assert.equal("PATCH", data.method);
        assert.equal("/method/patch", data.url);
    });
    it("should generate an url and provide DELETE method", function() {
        var data = jsRoutes.controllers.MethodController.delete();
        assert.equal("DELETE", data.method);
        assert.equal("/method/delete", data.url);
    });
    it("should generate an url and provide HEAD method", function() {
        var data = jsRoutes.controllers.MethodController.head();
        assert.equal("HEAD", data.method);
        assert.equal("/method/head", data.url);
    });
    it("should generate an url and provide OPTIONS method", function() {
        var data = jsRoutes.controllers.MethodController.options();
        assert.equal("OPTIONS", data.method);
        assert.equal("/method/options", data.url);
    });
});
