/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

var assert = require("assert");
var jsRoutes = require("./jsRoutes");

describe("The JavaScript router", function() {
    it("should generate an url for route with http request", function() {
        var data = jsRoutes.controllers.Application.async(10);
        assert.equal("/result/async?x=10", data.url);
    });
});
