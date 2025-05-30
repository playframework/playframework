/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

var assert = require("assert");
var jsRoutes = require("./jsRoutes");

describe("The JavaScript router for Double", function () {
    const defaultList = [1.1, 2.2, 3.3]
    const testList = [7.7, -8.8, 9.9e-10]
    const infinityList = [NaN, Infinity, -Infinity]
    it("should be correct for path param", function () {
        let path = "/double-p";
        var data = jsRoutes.controllers.DoubleController.path(null);
        assert.equal(path + "/null", data.url);
        data = jsRoutes.controllers.DoubleController.path();
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.DoubleController.path(undefined);
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.DoubleController.path(7.89);
        assert.equal(path + "/7.89", data.url);
        data = jsRoutes.controllers.DoubleController.path(-7.89);
        assert.equal(path + "/-7.89", data.url);
        data = jsRoutes.controllers.DoubleController.path(7.8e29);
        assert.equal(path + "/7.8e%2B29", data.url);
        data = jsRoutes.controllers.DoubleController.path(7.8e-29);
        assert.equal(path + "/7.8e-29", data.url);
        data = jsRoutes.controllers.DoubleController.path(NaN);
        assert.equal(path + "/NaN", data.url);
        data = jsRoutes.controllers.DoubleController.path(Infinity);
        assert.equal(path + "/Infinity", data.url);
        data = jsRoutes.controllers.DoubleController.path(-Infinity);
        assert.equal(path + "/-Infinity", data.url);
    });
    it("should be correct for query param", function () {
        let path = "/double";
        var data = jsRoutes.controllers.DoubleController.query(null);
        assert.equal(path + "?x=null", data.url);
        data = jsRoutes.controllers.DoubleController.query();
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.DoubleController.query(undefined);
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.DoubleController.query(7.89);
        assert.equal(path + "?x=7.89", data.url);
        data = jsRoutes.controllers.DoubleController.query(-7.89);
        assert.equal(path + "?x=-7.89", data.url);
        data = jsRoutes.controllers.DoubleController.query(7.8e29);
        assert.equal(path + "?x=7.8e%2B29", data.url);
        data = jsRoutes.controllers.DoubleController.query(7.8e-29);
        assert.equal(path + "?x=7.8e-29", data.url);
        data = jsRoutes.controllers.DoubleController.query(NaN);
        assert.equal(path + "?x=NaN", data.url);
        data = jsRoutes.controllers.DoubleController.query(Infinity);
        assert.equal(path + "?x=Infinity", data.url);
        data = jsRoutes.controllers.DoubleController.query(-Infinity);
        assert.equal(path + "?x=-Infinity", data.url);
    });
    it("should be correct for default query param", function () {
        let path = "/double-d";
        var data = jsRoutes.controllers.DoubleController.queryDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryDefault(7.89);
        assert.equal(path + "?x%3F%3D=7.89", data.url);
        data = jsRoutes.controllers.DoubleController.queryDefault(-7.89);
        assert.equal(path + "?x%3F%3D=-7.89", data.url);
        data = jsRoutes.controllers.DoubleController.queryDefault(7.8e29);
        assert.equal(path + "?x%3F%3D=7.8e%2B29", data.url);
        data = jsRoutes.controllers.DoubleController.queryDefault(7.8e-29);
        assert.equal(path + "?x%3F%3D=7.8e-29", data.url);
        data = jsRoutes.controllers.DoubleController.queryDefault(NaN);
        assert.equal(path + "?x%3F%3D=NaN", data.url);
        data = jsRoutes.controllers.DoubleController.queryDefault(Infinity);
        assert.equal(path + "?x%3F%3D=Infinity", data.url);
        data = jsRoutes.controllers.DoubleController.queryDefault(-Infinity);
        assert.equal(path + "?x%3F%3D=-Infinity", data.url);
    });
    it("should be correct for fixed query param", function () {
        let path = "/double-f";
        var data = jsRoutes.controllers.DoubleController.queryFixed(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryFixed();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryFixed(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryFixed(7.89);
        assert.equal(path, data.url);
    });
    it("should be correct for nullable query param", function () {
        let path = "/double-null";
        var data = jsRoutes.controllers.DoubleController.queryNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryNullable(7.89);
        assert.equal(path + "?x%3F=7.89", data.url);
        data = jsRoutes.controllers.DoubleController.queryNullable(-7.89);
        assert.equal(path + "?x%3F=-7.89", data.url);
        data = jsRoutes.controllers.DoubleController.queryNullable(7.8e29);
        assert.equal(path + "?x%3F=7.8e%2B29", data.url);
        data = jsRoutes.controllers.DoubleController.queryNullable(7.8e-29);
        assert.equal(path + "?x%3F=7.8e-29", data.url);
        data = jsRoutes.controllers.DoubleController.queryNullable(NaN);
        assert.equal(path + "?x%3F=NaN", data.url);
        data = jsRoutes.controllers.DoubleController.queryNullable(Infinity);
        assert.equal(path + "?x%3F=Infinity", data.url);
        data = jsRoutes.controllers.DoubleController.queryNullable(-Infinity);
        assert.equal(path + "?x%3F=-Infinity", data.url);
    });
    it("should be correct for optional query param", function () {
        let path = "/double-opt";
        var data = jsRoutes.controllers.DoubleController.queryOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryOptional(7.89);
        assert.equal(path + "?x%3F=7.89", data.url);
        data = jsRoutes.controllers.DoubleController.queryOptional(-7.89);
        assert.equal(path + "?x%3F=-7.89", data.url);
        data = jsRoutes.controllers.DoubleController.queryOptional(7.8e29);
        assert.equal(path + "?x%3F=7.8e%2B29", data.url);
        data = jsRoutes.controllers.DoubleController.queryOptional(7.8e-29);
        assert.equal(path + "?x%3F=7.8e-29", data.url);
        data = jsRoutes.controllers.DoubleController.queryOptional(NaN);
        assert.equal(path + "?x%3F=NaN", data.url);
        data = jsRoutes.controllers.DoubleController.queryOptional(Infinity);
        assert.equal(path + "?x%3F=Infinity", data.url);
        data = jsRoutes.controllers.DoubleController.queryOptional(-Infinity);
        assert.equal(path + "?x%3F=-Infinity", data.url);
    });
    it("should be correct for optional default query param", function () {
        let path = "/double-opt-d";
        var data = jsRoutes.controllers.DoubleController.queryOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryOptionalDefault(1.23);
        assert.equal(path + "?x%3F%3D=1.23", data.url);
        data = jsRoutes.controllers.DoubleController.queryOptionalDefault(7.89);
        assert.equal(path + "?x%3F%3D=7.89", data.url);
        data = jsRoutes.controllers.DoubleController.queryOptionalDefault(7.8e29);
        assert.equal(path + "?x%3F%3D=7.8e%2B29", data.url);
        data = jsRoutes.controllers.DoubleController.queryOptionalDefault(7.8e-29);
        assert.equal(path + "?x%3F%3D=7.8e-29", data.url);
        data = jsRoutes.controllers.DoubleController.queryOptionalDefault(NaN);
        assert.equal(path + "?x%3F%3D=NaN", data.url);
        data = jsRoutes.controllers.DoubleController.queryOptionalDefault(Infinity);
        assert.equal(path + "?x%3F%3D=Infinity", data.url);
        data = jsRoutes.controllers.DoubleController.queryOptionalDefault(-Infinity);
        assert.equal(path + "?x%3F%3D=-Infinity", data.url);
    });
    it("should be correct for list query param", function () {
        let path = "/double-list";
        var data = jsRoutes.controllers.DoubleController.queryList(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryList();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryList(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryList([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryList(testList);
        assert.equal(path + "?x%5B%5D=7.7&x%5B%5D=-8.8&x%5B%5D=9.9e-10", data.url);
        data = jsRoutes.controllers.DoubleController.queryList(infinityList);
        assert.equal(path + "?x%5B%5D=NaN&x%5B%5D=Infinity&x%5B%5D=-Infinity", data.url);
    });
    it("should be correct for list default query param", function () {
        let path = "/double-list-d";
        var data = jsRoutes.controllers.DoubleController.queryListDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3D=1.1&x%5B%5D%3D=2.2&x%5B%5D%3D=3.3", data.url);
        data = jsRoutes.controllers.DoubleController.queryListDefault(testList);
        assert.equal(path + "?x%5B%5D%3D=7.7&x%5B%5D%3D=-8.8&x%5B%5D%3D=9.9e-10", data.url);
        data = jsRoutes.controllers.DoubleController.queryListDefault(infinityList);
        assert.equal(path + "?x%5B%5D%3D=NaN&x%5B%5D%3D=Infinity&x%5B%5D%3D=-Infinity", data.url);
    });
    it("should be correct for list nullable query param", function () {
        let path = "/double-list-null";
        var data = jsRoutes.controllers.DoubleController.queryListNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListNullable([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListNullable(testList);
        assert.equal(path + "?x%5B%5D%3F=7.7&x%5B%5D%3F=-8.8&x%5B%5D%3F=9.9e-10", data.url);
        data = jsRoutes.controllers.DoubleController.queryListNullable(infinityList);
        assert.equal(path + "?x%5B%5D%3F=NaN&x%5B%5D%3F=Infinity&x%5B%5D%3F=-Infinity", data.url);
    });
    it("should be correct for list optional query param", function () {
        let path = "/double-list-opt";
        var data = jsRoutes.controllers.DoubleController.queryListOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListOptional([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListOptional(testList);
        assert.equal(path + "?x%5B%5D%3F=7.7&x%5B%5D%3F=-8.8&x%5B%5D%3F=9.9e-10", data.url);
        data = jsRoutes.controllers.DoubleController.queryListOptional(infinityList);
        assert.equal(path + "?x%5B%5D%3F=NaN&x%5B%5D%3F=Infinity&x%5B%5D%3F=-Infinity", data.url);
    });
    it("should be correct for list optional default query param", function () {
        let path = "/double-list-opt-d";
        var data = jsRoutes.controllers.DoubleController.queryListOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListOptionalDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.DoubleController.queryListOptionalDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3F%3D=1.1&x%5B%5D%3F%3D=2.2&x%5B%5D%3F%3D=3.3", data.url);
        data = jsRoutes.controllers.DoubleController.queryListOptionalDefault(testList);
        assert.equal(path + "?x%5B%5D%3F%3D=7.7&x%5B%5D%3F%3D=-8.8&x%5B%5D%3F%3D=9.9e-10", data.url);
        data = jsRoutes.controllers.DoubleController.queryListOptionalDefault(infinityList);
        assert.equal(path + "?x%5B%5D%3F%3D=NaN&x%5B%5D%3F%3D=Infinity&x%5B%5D%3F%3D=-Infinity", data.url);
    });
});
