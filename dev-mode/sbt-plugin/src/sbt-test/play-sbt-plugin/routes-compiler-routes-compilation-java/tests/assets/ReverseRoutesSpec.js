/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

var assert = require("assert");
var jsRoutes = require("./jsRoutes");

describe("The JavaScript router", function () {
    const testUUID = "2ef841cd-0fe0-423c-83ed-71040a1f42fe"
    const defaultUUID = "a90e582b-3c5b-4d37-b5f4-7730d2c672dd"

    it("should generate an url for route with http request", function () {
        var data = jsRoutes.controllers.Application.async(10);
        assert.equal("/result/async?x=10", data.url);
    });
    it("should generate an url for one method routes with non fixed params", function () {
        var data = jsRoutes.controllers.Application.reverse(
            false,
            'x',
            789,
            789,
            789,
            7.89,
            7.89,
            testUUID,
            789,
            789,
            7.89,
            "x",
            "x"
        );
        assert.equal("/reverse/non-fixed?b=false&c=x&s=789&i=789&l=789&f=7.89&d=7.89&uuid=" + testUUID + "&oi=789&ol=789&od=7.89&str=x&ostr=x", data.url);
    });
    it("should generate an url for one method routes with fixed params", function () {
        var data = jsRoutes.controllers.Application.reverse(
            true,
            'a',
            123,
            123,
            123,
            1.23,
            1.23,
            defaultUUID,
            123,
            123,
            1.23,
            "a",
            "a"
        );
        assert.equal("/reverse/fixed", data.url);
    });
});
