/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

var assert = require("assert");
var jsRoutes = require("./jsRoutes");

describe("The JavaScript router for Long", function () {
    const defaultList = [1, 2, 3]
    const testList = [7, -8, 3000000000]
    it("should be correct for path param", function () {
        let path = "/long-p";
        var data = jsRoutes.controllers.LongController.path(null);
        assert.equal(path + "/null", data.url);
        data = jsRoutes.controllers.LongController.path();
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.LongController.path(undefined);
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.LongController.path(789);
        assert.equal(path + "/789", data.url);
        data = jsRoutes.controllers.LongController.path(-789);
        assert.equal(path + "/-789", data.url);
    });
    it("should be correct for query param", function () {
        let path = "/long";
        var data = jsRoutes.controllers.LongController.query(null);
        assert.equal(path + "?x=null", data.url);
        data = jsRoutes.controllers.LongController.query();
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.LongController.query(undefined);
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.LongController.query(789);
        assert.equal(path + "?x=789", data.url);
        data = jsRoutes.controllers.LongController.query(-789);
        assert.equal(path + "?x=-789", data.url);
    });
    it("should be correct for default query param", function () {
        let path = "/long-d";
        var data = jsRoutes.controllers.LongController.queryDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryDefault(789);
        assert.equal(path + "?x%3F%3D=789", data.url);
        data = jsRoutes.controllers.LongController.queryDefault(-789);
        assert.equal(path + "?x%3F%3D=-789", data.url);
    });
    it("should be correct for fixed query param", function () {
        let path = "/long-f";
        var data = jsRoutes.controllers.LongController.queryFixed(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryFixed();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryFixed(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryFixed(789);
        assert.equal(path, data.url);
    });
    it("should be correct for nullable query param", function () {
        let path = "/long-null";
        var data = jsRoutes.controllers.LongController.queryNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryNullable(789);
        assert.equal(path + "?x%3F=789", data.url);
        data = jsRoutes.controllers.LongController.queryNullable(-789);
        assert.equal(path + "?x%3F=-789", data.url);
    });
    it("should be correct for optional query param", function () {
        let path = "/long-opt";
        var data = jsRoutes.controllers.LongController.queryOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryOptional(789);
        assert.equal(path + "?x%3F=789", data.url);
        data = jsRoutes.controllers.LongController.queryOptional(-789);
        assert.equal(path + "?x%3F=-789", data.url);
    });
    it("should be correct for optional default query param", function () {
        let path = "/long-opt-d";
        var data = jsRoutes.controllers.LongController.queryOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryOptionalDefault(123);
        assert.equal(path + "?x%3F%3D=123", data.url);
        data = jsRoutes.controllers.LongController.queryOptionalDefault(789);
        assert.equal(path + "?x%3F%3D=789", data.url);
    });
    it("should be correct for list query param", function () {
        let path = "/long-list";
        var data = jsRoutes.controllers.LongController.queryList(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryList();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryList(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryList([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryList(testList);
        assert.equal(path + "?x%5B%5D=7&x%5B%5D=-8&x%5B%5D=3000000000", data.url);
    });
    it("should be correct for list default query param", function () {
        let path = "/long-list-d";
        var data = jsRoutes.controllers.LongController.queryListDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3D=1&x%5B%5D%3D=2&x%5B%5D%3D=3", data.url);
        data = jsRoutes.controllers.LongController.queryListDefault(testList);
        assert.equal(path + "?x%5B%5D%3D=7&x%5B%5D%3D=-8&x%5B%5D%3D=3000000000", data.url);
    });
    it("should be correct for list nullable query param", function () {
        let path = "/long-list-null";
        var data = jsRoutes.controllers.LongController.queryListNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListNullable([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListNullable(testList);
        assert.equal(path + "?x%5B%5D%3F=7&x%5B%5D%3F=-8&x%5B%5D%3F=3000000000", data.url);
    });
    it("should be correct for list optional query param", function () {
        let path = "/long-list-opt";
        var data = jsRoutes.controllers.LongController.queryListOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListOptional([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListOptional(testList);
        assert.equal(path + "?x%5B%5D%3F=7&x%5B%5D%3F=-8&x%5B%5D%3F=3000000000", data.url);
    });
    it("should be correct for list optional default query param", function () {
        let path = "/long-list-opt-d";
        var data = jsRoutes.controllers.LongController.queryListOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListOptionalDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.LongController.queryListOptionalDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3F%3D=1&x%5B%5D%3F%3D=2&x%5B%5D%3F%3D=3", data.url);
        data = jsRoutes.controllers.LongController.queryListOptionalDefault(testList);
        assert.equal(path + "?x%5B%5D%3F%3D=7&x%5B%5D%3F%3D=-8&x%5B%5D%3F%3D=3000000000", data.url);
    });
});
