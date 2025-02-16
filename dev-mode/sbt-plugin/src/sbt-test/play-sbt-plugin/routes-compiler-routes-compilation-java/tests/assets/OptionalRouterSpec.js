/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

var assert = require("assert");
var jsRoutes = require("./jsRoutes");

describe("The JavaScript router for OptionalInt", function () {
    it("should be correct for query param", function () {
        let path = "/opt-int";
        var data = jsRoutes.controllers.OptionalController.queryInt(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryInt();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryInt(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryInt(789);
        assert.equal(path + "?x%3F=789", data.url);
        data = jsRoutes.controllers.OptionalController.queryInt(-789);
        assert.equal(path + "?x%3F=-789", data.url);
    });
    it("should be correct for default query param", function () {
        let path = "/opt-int-d";
        var data = jsRoutes.controllers.OptionalController.queryIntDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryIntDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryIntDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryIntDefault(123);
        assert.equal(path + "?x%3F%3D=123", data.url);
        data = jsRoutes.controllers.OptionalController.queryIntDefault(789);
        assert.equal(path + "?x%3F%3D=789", data.url);
    });
});

describe("The JavaScript router for OptionalLong", function () {
    it("should be correct for query param", function () {
        let path = "/opt-long";
        var data = jsRoutes.controllers.OptionalController.queryLong(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryLong();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryLong(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryLong(789);
        assert.equal(path + "?x%3F=789", data.url);
        data = jsRoutes.controllers.OptionalController.queryLong(-789);
        assert.equal(path + "?x%3F=-789", data.url);
    });
    it("should be correct for default query param", function () {
        let path = "/opt-long-d";
        var data = jsRoutes.controllers.OptionalController.queryLongDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryLongDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryLongDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryLongDefault(123);
        assert.equal(path + "?x%3F%3D=123", data.url);
        data = jsRoutes.controllers.OptionalController.queryLongDefault(789);
        assert.equal(path + "?x%3F%3D=789", data.url);
    });
});


describe("The JavaScript router for OptionalDouble", function () {
    it("should be correct for query param", function () {
        let path = "/opt-double";
        var data = jsRoutes.controllers.OptionalController.queryDouble(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryDouble();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryDouble(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryDouble(7.89);
        assert.equal(path + "?x%3F=7.89", data.url);
        data = jsRoutes.controllers.OptionalController.queryDouble(-7.89);
        assert.equal(path + "?x%3F=-7.89", data.url);
    });
    it("should be correct for default query param", function () {
        let path = "/opt-double-d";
        var data = jsRoutes.controllers.OptionalController.queryDoubleDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryDoubleDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryDoubleDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.OptionalController.queryDoubleDefault(1.23);
        assert.equal(path + "?x%3F%3D=1.23", data.url);
        data = jsRoutes.controllers.OptionalController.queryDoubleDefault(7.89);
        assert.equal(path + "?x%3F%3D=7.89", data.url);
    });
});
