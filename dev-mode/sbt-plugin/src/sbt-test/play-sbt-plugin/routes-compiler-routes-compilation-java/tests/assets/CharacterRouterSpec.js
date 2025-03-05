/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

var assert = require("assert");
var jsRoutes = require("./jsRoutes");

describe("The JavaScript router for Character", function () {
    const defaultList = ['a', 'b', 'c']
    const testList = ['x', 'y', 'z']
    const unicodeList = ['π', 'ε']
    it("should be correct for path param", function () {
        let path = "/char-p";
        var data = jsRoutes.controllers.CharacterController.path(null);
        assert.equal(path + "/null", data.url);
        data = jsRoutes.controllers.CharacterController.path();
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.CharacterController.path(undefined);
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.CharacterController.path('z');
        assert.equal(path + "/z", data.url);
        data = jsRoutes.controllers.CharacterController.path('π');
        assert.equal(path + "/%CF%80", data.url);
    });
    it("should be correct for query param", function () {
        let path = "/char";
        var data = jsRoutes.controllers.CharacterController.query(null);
        assert.equal(path + "?x=null", data.url);
        data = jsRoutes.controllers.CharacterController.query();
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.CharacterController.query(undefined);
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.CharacterController.query('z');
        assert.equal(path + "?x=z", data.url);
        data = jsRoutes.controllers.CharacterController.query('π');
        assert.equal(path + "?x=%CF%80", data.url);
    });
    it("should be correct for default query param", function () {
        let path = "/char-d";
        var data = jsRoutes.controllers.CharacterController.queryDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryDefault('a');
        assert.equal(path + "?x%3F%3D=a", data.url);
        data = jsRoutes.controllers.CharacterController.queryDefault('z');
        assert.equal(path + "?x%3F%3D=z", data.url);
        data = jsRoutes.controllers.CharacterController.queryDefault('π');
        assert.equal(path + "?x%3F%3D=%CF%80", data.url);
    });
    it("should be correct for fixed query param", function () {
        let path = "/char-f";
        var data = jsRoutes.controllers.CharacterController.queryFixed(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryFixed();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryFixed(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryFixed('z');
        assert.equal(path, data.url);
    });
    it("should be correct for nullable query param", function () {
        let path = "/char-null";
        var data = jsRoutes.controllers.CharacterController.queryNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryNullable('z');
        assert.equal(path + "?x%3F=z", data.url);
        data = jsRoutes.controllers.CharacterController.queryNullable('π');
        assert.equal(path + "?x%3F=%CF%80", data.url);
    });
    it("should be correct for optional query param", function () {
        let path = "/char-opt";
        var data = jsRoutes.controllers.CharacterController.queryOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryOptional('z');
        assert.equal(path + "?x%3F=z", data.url);
        data = jsRoutes.controllers.CharacterController.queryOptional('π');
        assert.equal(path + "?x%3F=%CF%80", data.url);
    });
    it("should be correct for optional default query param", function () {
        let path = "/char-opt-d";
        var data = jsRoutes.controllers.CharacterController.queryOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryOptionalDefault('a');
        assert.equal(path + "?x%3F%3D=a", data.url);
        data = jsRoutes.controllers.CharacterController.queryOptionalDefault('z');
        assert.equal(path + "?x%3F%3D=z", data.url);
        data = jsRoutes.controllers.CharacterController.queryOptionalDefault('π');
        assert.equal(path + "?x%3F%3D=%CF%80", data.url);
    });
    it("should be correct for list query param", function () {
        let path = "/char-list";
        var data = jsRoutes.controllers.CharacterController.queryList(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryList();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryList(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryList([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryList(defaultList);
        assert.equal(path + "?x%5B%5D=a&x%5B%5D=b&x%5B%5D=c", data.url);
        data = jsRoutes.controllers.CharacterController.queryList(unicodeList);
        assert.equal(path + "?x%5B%5D=%CF%80&x%5B%5D=%CE%B5", data.url);
    });
    it("should be correct for list default query param", function () {
        let path = "/char-list-d";
        var data = jsRoutes.controllers.CharacterController.queryListDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3D=a&x%5B%5D%3D=b&x%5B%5D%3D=c", data.url);
        data = jsRoutes.controllers.CharacterController.queryListDefault(testList);
        assert.equal(path + "?x%5B%5D%3D=x&x%5B%5D%3D=y&x%5B%5D%3D=z", data.url);
        data = jsRoutes.controllers.CharacterController.queryListDefault(unicodeList);
        assert.equal(path + "?x%5B%5D%3D=%CF%80&x%5B%5D%3D=%CE%B5", data.url);
    });
    it("should be correct for list nullable query param", function () {
        let path = "/char-list-null";
        var data = jsRoutes.controllers.CharacterController.queryListNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListNullable([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListNullable(testList);
        assert.equal(path + "?x%5B%5D%3F=x&x%5B%5D%3F=y&x%5B%5D%3F=z", data.url);
        data = jsRoutes.controllers.CharacterController.queryListNullable(unicodeList);
        assert.equal(path + "?x%5B%5D%3F=%CF%80&x%5B%5D%3F=%CE%B5", data.url);
    });
    it("should be correct for list optional query param", function () {
        let path = "/char-list-opt";
        var data = jsRoutes.controllers.CharacterController.queryListOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListOptional([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListOptional(testList);
        assert.equal(path + "?x%5B%5D%3F=x&x%5B%5D%3F=y&x%5B%5D%3F=z", data.url);
        data = jsRoutes.controllers.CharacterController.queryListOptional(unicodeList);
        assert.equal(path + "?x%5B%5D%3F=%CF%80&x%5B%5D%3F=%CE%B5", data.url);
    });
    it("should be correct for list optional default query param", function () {
        let path = "/char-list-opt-d";
        var data = jsRoutes.controllers.CharacterController.queryListOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListOptionalDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.CharacterController.queryListOptionalDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3F%3D=a&x%5B%5D%3F%3D=b&x%5B%5D%3F%3D=c", data.url);
        data = jsRoutes.controllers.CharacterController.queryListOptionalDefault(testList);
        assert.equal(path + "?x%5B%5D%3F%3D=x&x%5B%5D%3F%3D=y&x%5B%5D%3F%3D=z", data.url);
        data = jsRoutes.controllers.CharacterController.queryListOptionalDefault(unicodeList);
        assert.equal(path + "?x%5B%5D%3F%3D=%CF%80&x%5B%5D%3F%3D=%CE%B5", data.url);
    });
});
