/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

var assert = require("assert");
var jsRoutes = require("./jsRoutes");

describe("The JavaScript router for Short", function () {
    const defaultList = [1, 2, 3]
    const testList = [7, -8, 9]
    it("should be correct for path param", function () {
        let path = "/short-p";
        var data = jsRoutes.controllers.ShortController.path(null);
        assert.equal(path + "/null", data.url);
        data = jsRoutes.controllers.ShortController.path();
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.ShortController.path(undefined);
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.ShortController.path(789);
        assert.equal(path + "/789", data.url);
        data = jsRoutes.controllers.ShortController.path(-789);
        assert.equal(path + "/-789", data.url);
    });
    it("should be correct for query param", function () {
        let path = "/short";
        var data = jsRoutes.controllers.ShortController.query(null);
        assert.equal(path + "?x=null", data.url);
        data = jsRoutes.controllers.ShortController.query();
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.ShortController.query(undefined);
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.ShortController.query(789);
        assert.equal(path + "?x=789", data.url);
        data = jsRoutes.controllers.ShortController.query(-789);
        assert.equal(path + "?x=-789", data.url);
    });
    it("should be correct for default query param", function () {
        let path = "/short-d";
        var data = jsRoutes.controllers.ShortController.queryDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryDefault(789);
        assert.equal(path + "?x%3F%3D=789", data.url);
        data = jsRoutes.controllers.ShortController.queryDefault(-789);
        assert.equal(path + "?x%3F%3D=-789", data.url);
    });
    it("should be correct for fixed query param", function () {
        let path = "/short-f";
        var data = jsRoutes.controllers.ShortController.queryFixed(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryFixed();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryFixed(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryFixed(789);
        assert.equal(path, data.url);
    });
    it("should be correct for nullable query param", function () {
        let path = "/short-null";
        var data = jsRoutes.controllers.ShortController.queryNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryNullable(789);
        assert.equal(path + "?x%3F=789", data.url);
        data = jsRoutes.controllers.ShortController.queryNullable(-789);
        assert.equal(path + "?x%3F=-789", data.url);
    });
    it("should be correct for optional query param", function () {
        let path = "/short-opt";
        var data = jsRoutes.controllers.ShortController.queryOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryOptional(789);
        assert.equal(path + "?x%3F=789", data.url);
        data = jsRoutes.controllers.ShortController.queryOptional(-789);
        assert.equal(path + "?x%3F=-789", data.url);
    });
    it("should be correct for optional default query param", function () {
        let path = "/short-opt-d";
        var data = jsRoutes.controllers.ShortController.queryOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryOptionalDefault(123);
        assert.equal(path + "?x%3F%3D=123", data.url);
        data = jsRoutes.controllers.ShortController.queryOptionalDefault(789);
        assert.equal(path + "?x%3F%3D=789", data.url);
    });
    it("should be correct for list query param", function () {
        let path = "/short-list";
        var data = jsRoutes.controllers.ShortController.queryList(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryList();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryList(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryList([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryList(testList);
        assert.equal(path + "?x%5B%5D=7&x%5B%5D=-8&x%5B%5D=9", data.url);
    });
    it("should be correct for list default query param", function () {
        let path = "/short-list-d";
        var data = jsRoutes.controllers.ShortController.queryListDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3D=1&x%5B%5D%3D=2&x%5B%5D%3D=3", data.url);
        data = jsRoutes.controllers.ShortController.queryListDefault(testList);
        assert.equal(path + "?x%5B%5D%3D=7&x%5B%5D%3D=-8&x%5B%5D%3D=9", data.url);
    });
    it("should be correct for list nullable query param", function () {
        let path = "/short-list-null";
        var data = jsRoutes.controllers.ShortController.queryListNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListNullable([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListNullable(testList);
        assert.equal(path + "?x%5B%5D%3F=7&x%5B%5D%3F=-8&x%5B%5D%3F=9", data.url);
    });
    it("should be correct for list optional query param", function () {
        let path = "/short-list-opt";
        var data = jsRoutes.controllers.ShortController.queryListOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListOptional([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListOptional(testList);
        assert.equal(path + "?x%5B%5D%3F=7&x%5B%5D%3F=-8&x%5B%5D%3F=9", data.url);
    });
    it("should be correct for list optional default query param", function () {
        let path = "/short-list-opt-d";
        var data = jsRoutes.controllers.ShortController.queryListOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListOptionalDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.ShortController.queryListOptionalDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3F%3D=1&x%5B%5D%3F%3D=2&x%5B%5D%3F%3D=3", data.url);
        data = jsRoutes.controllers.ShortController.queryListOptionalDefault(testList);
        assert.equal(path + "?x%5B%5D%3F%3D=7&x%5B%5D%3F%3D=-8&x%5B%5D%3F%3D=9", data.url);
    });
});
