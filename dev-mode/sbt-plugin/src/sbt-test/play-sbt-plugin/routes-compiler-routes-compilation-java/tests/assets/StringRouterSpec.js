/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

var assert = require("assert");
var jsRoutes = require("./jsRoutes");

describe("The JavaScript router for String", function () {
    const defaultList = ['abc', 'def', 'ghi']
    const testList = ['ab', 'cd', 'ef']
    const unicodeList = ['πε', 'επ']
    it("should be correct for path param", function () {
        let path = "/str-p";
        var data = jsRoutes.controllers.StringController.path(null);
        assert.equal(path + "/null", data.url);
        data = jsRoutes.controllers.StringController.path();
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.StringController.path(undefined);
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.StringController.path('xyz');
        assert.equal(path + "/xyz", data.url);
        data = jsRoutes.controllers.StringController.path('πε');
        assert.equal(path + "/%CF%80%CE%B5", data.url);
    });
    it("should be correct for query param", function () {
        let path = "/str";
        var data = jsRoutes.controllers.StringController.query(null);
        assert.equal(path + "?x=null", data.url);
        data = jsRoutes.controllers.StringController.query();
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.StringController.query(undefined);
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.StringController.query('xyz');
        assert.equal(path + "?x=xyz", data.url);
        data = jsRoutes.controllers.StringController.query('πε');
        assert.equal(path + "?x=%CF%80%CE%B5", data.url);
    });
    it("should be correct for default query param", function () {
        let path = "/str-d";
        var data = jsRoutes.controllers.StringController.queryDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryDefault('abc');
        assert.equal(path + "?x%3F%3D=abc", data.url);
        data = jsRoutes.controllers.StringController.queryDefault('xyz');
        assert.equal(path + "?x%3F%3D=xyz", data.url);
        data = jsRoutes.controllers.StringController.queryDefault('πε');
        assert.equal(path + "?x%3F%3D=%CF%80%CE%B5", data.url);
    });
    it("should be correct for fixed query param", function () {
        let path = "/str-f";
        var data = jsRoutes.controllers.StringController.queryFixed(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryFixed();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryFixed(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryFixed('xyz');
        assert.equal(path, data.url);
    });
    it("should be correct for nullable query param", function () {
        let path = "/str-null";
        var data = jsRoutes.controllers.StringController.queryNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryNullable('xyz');
        assert.equal(path + "?x%3F=xyz", data.url);
        data = jsRoutes.controllers.StringController.queryNullable('πε');
        assert.equal(path + "?x%3F=%CF%80%CE%B5", data.url);
    });
    it("should be correct for optional query param", function () {
        let path = "/str-opt";
        var data = jsRoutes.controllers.StringController.queryOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryOptional('xyz');
        assert.equal(path + "?x%3F=xyz", data.url);
        data = jsRoutes.controllers.StringController.queryOptional('πε');
        assert.equal(path + "?x%3F=%CF%80%CE%B5", data.url);
    });
    it("should be correct for optional default query param", function () {
        let path = "/str-opt-d";
        var data = jsRoutes.controllers.StringController.queryOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryOptionalDefault('abc');
        assert.equal(path + "?x%3F%3D=abc", data.url);
        data = jsRoutes.controllers.StringController.queryOptionalDefault('xyz');
        assert.equal(path + "?x%3F%3D=xyz", data.url);
        data = jsRoutes.controllers.StringController.queryOptionalDefault('πε');
        assert.equal(path + "?x%3F%3D=%CF%80%CE%B5", data.url);
    });
    it("should be correct for list query param", function () {
        let path = "/str-list";
        var data = jsRoutes.controllers.StringController.queryList(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryList();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryList(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryList([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryList(defaultList);
        assert.equal(path + "?x%5B%5D=abc&x%5B%5D=def&x%5B%5D=ghi", data.url);
        data = jsRoutes.controllers.StringController.queryList(unicodeList);
        assert.equal(path + "?x%5B%5D=%CF%80%CE%B5&x%5B%5D=%CE%B5%CF%80", data.url);
    });
    it("should be correct for list default query param", function () {
        let path = "/str-list-d";
        var data = jsRoutes.controllers.StringController.queryListDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3D=abc&x%5B%5D%3D=def&x%5B%5D%3D=ghi", data.url);
        data = jsRoutes.controllers.StringController.queryListDefault(testList);
        assert.equal(path + "?x%5B%5D%3D=ab&x%5B%5D%3D=cd&x%5B%5D%3D=ef", data.url);
        data = jsRoutes.controllers.StringController.queryListDefault(unicodeList);
        assert.equal(path + "?x%5B%5D%3D=%CF%80%CE%B5&x%5B%5D%3D=%CE%B5%CF%80", data.url);
    });
    it("should be correct for list nullable query param", function () {
        let path = "/str-list-null";
        var data = jsRoutes.controllers.StringController.queryListNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListNullable([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListNullable(testList);
        assert.equal(path + "?x%5B%5D%3F=ab&x%5B%5D%3F=cd&x%5B%5D%3F=ef", data.url);
        data = jsRoutes.controllers.StringController.queryListNullable(unicodeList);
        assert.equal(path + "?x%5B%5D%3F=%CF%80%CE%B5&x%5B%5D%3F=%CE%B5%CF%80", data.url);
    });
    it("should be correct for list optional query param", function () {
        let path = "/str-list-opt";
        var data = jsRoutes.controllers.StringController.queryListOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListOptional([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListOptional(testList);
        assert.equal(path + "?x%5B%5D%3F=ab&x%5B%5D%3F=cd&x%5B%5D%3F=ef", data.url);
        data = jsRoutes.controllers.StringController.queryListOptional(unicodeList);
        assert.equal(path + "?x%5B%5D%3F=%CF%80%CE%B5&x%5B%5D%3F=%CE%B5%CF%80", data.url);
    });
    it("should be correct for list optional default query param", function () {
        let path = "/str-list-opt-d";
        var data = jsRoutes.controllers.StringController.queryListOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListOptionalDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.StringController.queryListOptionalDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3F%3D=abc&x%5B%5D%3F%3D=def&x%5B%5D%3F%3D=ghi", data.url);
        data = jsRoutes.controllers.StringController.queryListOptionalDefault(testList);
        assert.equal(path + "?x%5B%5D%3F%3D=ab&x%5B%5D%3F%3D=cd&x%5B%5D%3F%3D=ef", data.url);
        data = jsRoutes.controllers.StringController.queryListOptionalDefault(unicodeList);
        assert.equal(path + "?x%5B%5D%3F%3D=%CF%80%CE%B5&x%5B%5D%3F%3D=%CE%B5%CF%80", data.url);
    });
});
