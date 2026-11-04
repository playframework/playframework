/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

var assert = require("assert");
var jsRoutes = require("./jsRoutes");

describe("The JavaScript router", function() {
    it("should generate a url for assets", function() {
        var data = jsRoutes.router.controllers.Assets.versioned('hello.png');
        assert.equal("/public/hello.png", data.url);
    });
    it("should provide the GET method for assets", function() {
        var data = jsRoutes.router.controllers.Assets.versioned();
        assert.equal("GET", data.method);
    });
});
