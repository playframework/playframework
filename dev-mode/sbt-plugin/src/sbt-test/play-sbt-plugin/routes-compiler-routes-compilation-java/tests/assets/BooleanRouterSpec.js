/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

var assert = require("assert");
var jsRoutes = require("./jsRoutes");

describe("The JavaScript router for Boolean", function() {
    it("should be correct for path param", function() {
        let path = "/bool-p";
        var data = jsRoutes.controllers.BooleanController.path(null);
        assert.equal(path + "/false", data.url);
        data = jsRoutes.controllers.BooleanController.path();
        assert.equal(path + "/false", data.url);
        data = jsRoutes.controllers.BooleanController.path(undefined);
        assert.equal(path + "/false", data.url);
        data = jsRoutes.controllers.BooleanController.path(true);
        assert.equal(path + "/true", data.url);
        data = jsRoutes.controllers.BooleanController.path(false);
        assert.equal(path + "/false", data.url);
        data = jsRoutes.controllers.BooleanController.path(1);
        assert.equal(path + "/true", data.url);
        data = jsRoutes.controllers.BooleanController.path(0);
        assert.equal(path + "/false", data.url);
    });
    it("should be correct for query param", function() {
        let path = "/bool";
        var data = jsRoutes.controllers.BooleanController.query(null);
        assert.equal(path + "?x=false", data.url);
        data = jsRoutes.controllers.BooleanController.query();
        assert.equal(path + "?x=false", data.url);
        data = jsRoutes.controllers.BooleanController.query(undefined);
        assert.equal(path + "?x=false", data.url);
        data = jsRoutes.controllers.BooleanController.query(true);
        assert.equal(path + "?x=true", data.url);
        data = jsRoutes.controllers.BooleanController.query(false);
        assert.equal(path + "?x=false", data.url);
        data = jsRoutes.controllers.BooleanController.query(1);
        assert.equal(path + "?x=true", data.url);
        data = jsRoutes.controllers.BooleanController.query(0);
        assert.equal(path + "?x=false", data.url);
    });
    it("should be correct for default query param", function() {
        let path = "/bool-d";
        var data = jsRoutes.controllers.BooleanController.queryDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryDefault(true);
        assert.equal(path + "?x%3F%3D=true", data.url);
        data = jsRoutes.controllers.BooleanController.queryDefault(false);
        assert.equal(path + "?x%3F%3D=false", data.url);
        data = jsRoutes.controllers.BooleanController.queryDefault(1);
        assert.equal(path + "?x%3F%3D=true", data.url);
        data = jsRoutes.controllers.BooleanController.queryDefault(0);
        assert.equal(path + "?x%3F%3D=false", data.url);
    });
    it("should be correct for fixed query param", function() {
        let path = "/bool-f";
        var data = jsRoutes.controllers.BooleanController.queryFixed(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryFixed();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryFixed(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryFixed(true);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryFixed(false);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryFixed(1);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryFixed(0);
        assert.equal(path, data.url);
    });
    it("should be correct for nullable query param", function() {
        let path = "/bool-null";
        var data = jsRoutes.controllers.BooleanController.queryNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryNullable(true);
        assert.equal(path + "?x%3F=true", data.url);
        data = jsRoutes.controllers.BooleanController.queryNullable(false);
        assert.equal(path + "?x%3F=false", data.url);
        data = jsRoutes.controllers.BooleanController.queryNullable(1);
        assert.equal(path + "?x%3F=true", data.url);
        data = jsRoutes.controllers.BooleanController.queryNullable(0);
        assert.equal(path + "?x%3F=false", data.url);
    });
    it("should be correct for optional query param", function() {
        let path = "/bool-opt";
        var data = jsRoutes.controllers.BooleanController.queryOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryOptional(true);
        assert.equal(path + "?x%3F=true", data.url);
        data = jsRoutes.controllers.BooleanController.queryOptional(false);
        assert.equal(path + "?x%3F=false", data.url);
        data = jsRoutes.controllers.BooleanController.queryOptional(1);
        assert.equal(path + "?x%3F=true", data.url);
        data = jsRoutes.controllers.BooleanController.queryOptional(0);
        assert.equal(path + "?x%3F=false", data.url);
    });
    it("should be correct for optional default query param", function() {
        let path = "/bool-opt-d";
        var data = jsRoutes.controllers.BooleanController.queryOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryOptionalDefault(true);
        assert.equal(path + "?x%3F%3D=true", data.url);
        data = jsRoutes.controllers.BooleanController.queryOptionalDefault(false);
        assert.equal(path + "?x%3F%3D=false", data.url);
        data = jsRoutes.controllers.BooleanController.queryOptionalDefault(1);
        assert.equal(path + "?x%3F%3D=true", data.url);
        data = jsRoutes.controllers.BooleanController.queryOptionalDefault(0);
        assert.equal(path + "?x%3F%3D=false", data.url);
    });
    it("should be correct for list query param", function() {
        let path = "/bool-list";
        var data = jsRoutes.controllers.BooleanController.queryList(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryList();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryList(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryList([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryList([true, false, true]);
        assert.equal(path + "?x%5B%5D=true&x%5B%5D=false&x%5B%5D=true", data.url);
        data = jsRoutes.controllers.BooleanController.queryList([1, 0, 1]);
        assert.equal(path + "?x%5B%5D=true&x%5B%5D=false&x%5B%5D=true", data.url);
    });
    it("should be correct for list default query param", function() {
        let path = "/bool-list-d";
        var data = jsRoutes.controllers.BooleanController.queryListDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListDefault([true, false, true]);
        assert.equal(path + "?x%5B%5D%3D=true&x%5B%5D%3D=false&x%5B%5D%3D=true", data.url);
        data = jsRoutes.controllers.BooleanController.queryListDefault([1, 0, 1]);
        assert.equal(path + "?x%5B%5D%3D=true&x%5B%5D%3D=false&x%5B%5D%3D=true", data.url);
    });
    it("should be correct for list nullable query param", function() {
        let path = "/bool-list-null";
        var data = jsRoutes.controllers.BooleanController.queryListNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListNullable([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListNullable([true, false, true]);
        assert.equal(path + "?x%5B%5D%3F=true&x%5B%5D%3F=false&x%5B%5D%3F=true", data.url);
        data = jsRoutes.controllers.BooleanController.queryListNullable([1, 0, 1]);
        assert.equal(path + "?x%5B%5D%3F=true&x%5B%5D%3F=false&x%5B%5D%3F=true", data.url);
    });
    it("should be correct for list optional query param", function() {
        let path = "/bool-list-opt";
        var data = jsRoutes.controllers.BooleanController.queryListOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListOptional([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListOptional([true, false, true]);
        assert.equal(path + "?x%5B%5D%3F=true&x%5B%5D%3F=false&x%5B%5D%3F=true", data.url);
        data = jsRoutes.controllers.BooleanController.queryListOptional([1, 0, 1]);
        assert.equal(path + "?x%5B%5D%3F=true&x%5B%5D%3F=false&x%5B%5D%3F=true", data.url);
    });
    it("should be correct for list optional default query param", function() {
        let path = "/bool-list-opt-d";
        var data = jsRoutes.controllers.BooleanController.queryListOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListOptionalDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.BooleanController.queryListOptionalDefault([true, false, true]);
        assert.equal(path + "?x%5B%5D%3F%3D=true&x%5B%5D%3F%3D=false&x%5B%5D%3F%3D=true", data.url);
        data = jsRoutes.controllers.BooleanController.queryListOptionalDefault([1, 0, 1]);
        assert.equal(path + "?x%5B%5D%3F%3D=true&x%5B%5D%3F%3D=false&x%5B%5D%3F%3D=true", data.url);
    });
});
