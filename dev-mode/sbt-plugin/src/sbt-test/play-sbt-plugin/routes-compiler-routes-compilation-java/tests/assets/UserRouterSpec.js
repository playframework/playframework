/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

var assert = require("assert");
var jsRoutes = require("./jsRoutes");

class User {
    constructor(id) {
        this.id = id;
    }
}

describe("The JavaScript router for User", function () {
    const defaultList = [new User('111'), new User('222')]
    const testList = [new User('333'), new User('444')]
    it("should be correct for path param", function () {
        let path = "/user-p";
        var data = jsRoutes.controllers.UserController.path(null);
        assert.equal(path + "/null", data.url);
        data = jsRoutes.controllers.UserController.path();
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.UserController.path(undefined);
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.UserController.path(new User("foo%bar"));
        assert.equal(path + "/foo%25bar", data.url);
    });
    it("should be correct for query param", function () {
        let path = "/user";
        var data = jsRoutes.controllers.UserController.query(null);
        assert.equal(path + "?x=null", data.url);
        data = jsRoutes.controllers.UserController.query();
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.UserController.query(undefined);
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.UserController.query(new User("πε"));
        assert.equal(path + "?x=%CF%80%CE%B5", data.url);
    });
    it("should be correct for default query param", function () {
        let path = "/user-d";
        var data = jsRoutes.controllers.UserController.queryDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryDefault(new User('789'));
        assert.equal(path + "?x%3F%3D=789", data.url);
    });
    it("should be correct for fixed query param", function () {
        let path = "/user-f";
        var data = jsRoutes.controllers.UserController.queryFixed(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryFixed();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryFixed(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryFixed(new User('789'));
        assert.equal(path, data.url);
    });
    it("should be correct for nullable query param", function () {
        let path = "/user-null";
        var data = jsRoutes.controllers.UserController.queryNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryNullable(new User('789'));
        assert.equal(path + "?x%3F=789", data.url);
    });
    it("should be correct for optional query param", function () {
        let path = "/user-opt";
        var data = jsRoutes.controllers.UserController.queryOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryOptional(new User('789'));
        assert.equal(path + "?x%3F=789", data.url);
    });
    it("should be correct for optional default query param", function () {
        let path = "/user-opt-d";
        var data = jsRoutes.controllers.UserController.queryOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryOptionalDefault(new User('123'));
        assert.equal(path + "?x%3F%3D=123", data.url);
        data = jsRoutes.controllers.UserController.queryOptionalDefault(new User('789'));
        assert.equal(path + "?x%3F%3D=789", data.url);
    });
    it("should be correct for list query param", function () {
        let path = "/user-list";
        var data = jsRoutes.controllers.UserController.queryList(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryList();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryList(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryList([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryList(testList);
        assert.equal(path + "?x%5B%5D=333&x%5B%5D=444", data.url);
    });
    it("should be correct for list default query param", function () {
        let path = "/user-list-d";
        var data = jsRoutes.controllers.UserController.queryListDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3D=111&x%5B%5D%3D=222", data.url);
        data = jsRoutes.controllers.UserController.queryListDefault(testList);
        assert.equal(path + "?x%5B%5D%3D=333&x%5B%5D%3D=444", data.url);
    });
    it("should be correct for list nullable query param", function () {
        let path = "/user-list-null";
        var data = jsRoutes.controllers.UserController.queryListNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListNullable([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListNullable(testList);
        assert.equal(path + "?x%5B%5D%3F=333&x%5B%5D%3F=444", data.url);
    });
    it("should be correct for list optional query param", function () {
        let path = "/user-list-opt";
        var data = jsRoutes.controllers.UserController.queryListOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListOptional([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListOptional(testList);
        assert.equal(path + "?x%5B%5D%3F=333&x%5B%5D%3F=444", data.url);
    });
    it("should be correct for list optional default query param", function () {
        let path = "/user-list-opt-d";
        var data = jsRoutes.controllers.UserController.queryListOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListOptionalDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UserController.queryListOptionalDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3F%3D=111&x%5B%5D%3F%3D=222", data.url);
        data = jsRoutes.controllers.UserController.queryListOptionalDefault(testList);
        assert.equal(path + "?x%5B%5D%3F%3D=333&x%5B%5D%3F%3D=444", data.url);
    });
});
