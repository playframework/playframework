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
    it("should avoid name collisions on query string with complex names", function() {
        var data = jsRoutes.controllers.Application.sameEscapedJavaIdentifier("a", "b");
        assert.equal("/same-escaped-java-identifier?" + encodeURI('[]') + "=a&" + encodeURI('%%') + "=b", data.url);
    });
    it("should map differently named path parameters to the reverse signature", function() {
        assert.equal("/reverse/renamed/fixed", jsRoutes.controllers.Application.renamedParams("fixed").url);
        assert.equal("/reverse/renamed/other", jsRoutes.controllers.Application.renamedParams("other").url);
    });
    it("should map differently named query parameters and their defaults", function() {
        assert.equal("/reverse/query/fixed", jsRoutes.controllers.Application.renamedQueryParam("fixed").url);
        assert.equal("/reverse/query", jsRoutes.controllers.Application.renamedQueryParam(null).url);
        assert.equal("/reverse/query?alias=other", jsRoutes.controllers.Application.renamedQueryParam("other").url);
    });
    it("should omit parameters fixed to the same value in every route", function() {
        assert.equal("/reverse/shared/one", jsRoutes.controllers.Application.sharedFixedParams("one").url);
        assert.equal("/reverse/shared/two", jsRoutes.controllers.Application.sharedFixedParams("two").url);
        assert.equal("/reverse/shared", jsRoutes.controllers.Application.sharedFixedParams(null).url);
        assert.equal("/reverse/shared?second=other", jsRoutes.controllers.Application.sharedFixedParams("other").url);
    });
    it("should handle actions with only constrained reverse routes", function() {
        assert.equal("/reverse/fixed-only/one", jsRoutes.controllers.Application.fixedOnlyParams("one").url);
        assert.equal("/reverse/fixed-only/two", jsRoutes.controllers.Application.fixedOnlyParams("two").url);
        assert.equal(undefined, jsRoutes.controllers.Application.fixedOnlyParams("other"));
    });
    it("should ignore routes after an unconstrained reverse match", function() {
        assert.equal("/reverse/unconstrained/fixed", jsRoutes.controllers.Application.unconstrainedFirst("fixed").url);
    });
});
