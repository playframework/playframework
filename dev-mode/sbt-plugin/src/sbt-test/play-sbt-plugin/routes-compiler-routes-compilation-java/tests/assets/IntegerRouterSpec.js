/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

var assert = require("assert");
var jsRoutes = require("./jsRoutes");

describe("The JavaScript router for Integer", function () {
    const defaultList = [1, 2, 3]
    const testList = [7, -8, 65536]
    it("should be correct for path param", function () {
        let path = "/int-p";
        var data = jsRoutes.controllers.IntegerController.path(null);
        assert.equal(path + "/null", data.url);
        data = jsRoutes.controllers.IntegerController.path();
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.IntegerController.path(undefined);
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.IntegerController.path(789);
        assert.equal(path + "/789", data.url);
        data = jsRoutes.controllers.IntegerController.path(-789);
        assert.equal(path + "/-789", data.url);
    });
    it("should be correct for query param", function () {
        let path = "/int";
        var data = jsRoutes.controllers.IntegerController.query(null);
        assert.equal(path + "?x=null", data.url);
        data = jsRoutes.controllers.IntegerController.query();
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.IntegerController.query(undefined);
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.IntegerController.query(789);
        assert.equal(path + "?x=789", data.url);
        data = jsRoutes.controllers.IntegerController.query(-789);
        assert.equal(path + "?x=-789", data.url);
    });
    it("should be correct for default query param", function () {
        let path = "/int-d";
        var data = jsRoutes.controllers.IntegerController.queryDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryDefault(789);
        assert.equal(path + "?x%3F%3D=789", data.url);
        data = jsRoutes.controllers.IntegerController.queryDefault(-789);
        assert.equal(path + "?x%3F%3D=-789", data.url);
    });
    it("should be correct for fixed query param", function () {
        let path = "/int-f";
        var data = jsRoutes.controllers.IntegerController.queryFixed(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryFixed();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryFixed(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryFixed(789);
        assert.equal(path, data.url);
    });
    it("should be correct for nullable query param", function () {
        let path = "/int-null";
        var data = jsRoutes.controllers.IntegerController.queryNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryNullable(789);
        assert.equal(path + "?x%3F=789", data.url);
        data = jsRoutes.controllers.IntegerController.queryNullable(-789);
        assert.equal(path + "?x%3F=-789", data.url);
    });
    it("should be correct for optional query param", function () {
        let path = "/int-opt";
        var data = jsRoutes.controllers.IntegerController.queryOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryOptional(789);
        assert.equal(path + "?x%3F=789", data.url);
        data = jsRoutes.controllers.IntegerController.queryOptional(-789);
        assert.equal(path + "?x%3F=-789", data.url);
    });
    it("should be correct for optional default query param", function () {
        let path = "/int-opt-d";
        var data = jsRoutes.controllers.IntegerController.queryOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryOptionalDefault(123);
        assert.equal(path + "?x%3F%3D=123", data.url);
        data = jsRoutes.controllers.IntegerController.queryOptionalDefault(789);
        assert.equal(path + "?x%3F%3D=789", data.url);
    });
    it("should be correct for list query param", function () {
        let path = "/int-list";
        var data = jsRoutes.controllers.IntegerController.queryList(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryList();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryList(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryList([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryList(testList);
        assert.equal(path + "?x%5B%5D=7&x%5B%5D=-8&x%5B%5D=65536", data.url);
    });
    it("should be correct for list default query param", function () {
        let path = "/int-list-d";
        var data = jsRoutes.controllers.IntegerController.queryListDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3D=1&x%5B%5D%3D=2&x%5B%5D%3D=3", data.url);
        data = jsRoutes.controllers.IntegerController.queryListDefault(testList);
        assert.equal(path + "?x%5B%5D%3D=7&x%5B%5D%3D=-8&x%5B%5D%3D=65536", data.url);
    });
    it("should be correct for list nullable query param", function () {
        let path = "/int-list-null";
        var data = jsRoutes.controllers.IntegerController.queryListNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListNullable([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListNullable(testList);
        assert.equal(path + "?x%5B%5D%3F=7&x%5B%5D%3F=-8&x%5B%5D%3F=65536", data.url);
    });
    it("should be correct for list optional query param", function () {
        let path = "/int-list-opt";
        var data = jsRoutes.controllers.IntegerController.queryListOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListOptional([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListOptional(testList);
        assert.equal(path + "?x%5B%5D%3F=7&x%5B%5D%3F=-8&x%5B%5D%3F=65536", data.url);
    });
    it("should be correct for list optional default query param", function () {
        let path = "/int-list-opt-d";
        var data = jsRoutes.controllers.IntegerController.queryListOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListOptionalDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.IntegerController.queryListOptionalDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3F%3D=1&x%5B%5D%3F%3D=2&x%5B%5D%3F%3D=3", data.url);
        data = jsRoutes.controllers.IntegerController.queryListOptionalDefault(testList);
        assert.equal(path + "?x%5B%5D%3F%3D=7&x%5B%5D%3F%3D=-8&x%5B%5D%3F%3D=65536", data.url);
    });
});
