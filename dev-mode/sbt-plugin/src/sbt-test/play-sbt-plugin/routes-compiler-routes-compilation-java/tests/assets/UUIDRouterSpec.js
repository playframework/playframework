/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

var assert = require("assert");
var jsRoutes = require("./jsRoutes");

describe("The JavaScript router for UUID", function () {
    const defaultUUID = "a90e582b-3c5b-4d37-b5f4-7730d2c672dd"
    const testUUID = "2ef841cd-0fe0-423c-83ed-71040a1f42fe"
    const defaultList = [defaultUUID, "fff71165-0ae2-47d0-9151-3afe742c6351"]
    const testList = [testUUID, "5dee2e50-32c3-4ffc-a57c-18014e5eaf82"]
    it("should be correct for path param", function () {
        let path = "/uuid-p";
        var data = jsRoutes.controllers.UUIDController.path(null);
        assert.equal(path + "/null", data.url);
        data = jsRoutes.controllers.UUIDController.path();
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.UUIDController.path(undefined);
        assert.equal(path + "/undefined", data.url);
        data = jsRoutes.controllers.UUIDController.path(testUUID);
        assert.equal(path + "/" + testUUID, data.url);
    });
    it("should be correct for query param", function () {
        let path = "/uuid";
        var data = jsRoutes.controllers.UUIDController.query(null);
        assert.equal(path + "?x=null", data.url);
        data = jsRoutes.controllers.UUIDController.query();
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.UUIDController.query(undefined);
        assert.equal(path + "?x=undefined", data.url);
        data = jsRoutes.controllers.UUIDController.query(testUUID);
        assert.equal(path + "?x=" + testUUID, data.url);
    });
    it("should be correct for default query param", function () {
        let path = "/uuid-d";
        var data = jsRoutes.controllers.UUIDController.queryDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryDefault(testUUID);
        assert.equal(path + "?x%3F%3D=" + testUUID, data.url);
    });
    it("should be correct for fixed query param", function () {
        let path = "/uuid-f";
        var data = jsRoutes.controllers.UUIDController.queryFixed(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryFixed();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryFixed(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryFixed(testUUID);
        assert.equal(path, data.url);
    });
    it("should be correct for nullable query param", function () {
        let path = "/uuid-null";
        var data = jsRoutes.controllers.UUIDController.queryNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryNullable(testUUID);
        assert.equal(path + "?x%3F=" + testUUID, data.url);
    });
    it("should be correct for optional query param", function () {
        let path = "/uuid-opt";
        var data = jsRoutes.controllers.UUIDController.queryOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryOptional(testUUID);
        assert.equal(path + "?x%3F=" + testUUID, data.url);
    });
    it("should be correct for optional default query param", function () {
        let path = "/uuid-opt-d";
        var data = jsRoutes.controllers.UUIDController.queryOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryOptionalDefault(defaultUUID);
        assert.equal(path + "?x%3F%3D=" + defaultUUID, data.url);
        data = jsRoutes.controllers.UUIDController.queryOptionalDefault(testUUID);
        assert.equal(path + "?x%3F%3D=" + testUUID, data.url);
    });
    it("should be correct for list query param", function () {
        let path = "/uuid-list";
        var data = jsRoutes.controllers.UUIDController.queryList(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryList();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryList(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryList([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryList(testList);
        assert.equal(path + "?x%5B%5D=" + testList[0] + "&x%5B%5D=" + testList[1], data.url);
    });
    it("should be correct for list default query param", function () {
        let path = "/uuid-list-d";
        var data = jsRoutes.controllers.UUIDController.queryListDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3D=" + defaultList[0] + "&x%5B%5D%3D=" + defaultList[1], data.url);
        data = jsRoutes.controllers.UUIDController.queryListDefault(testList);
        assert.equal(path + "?x%5B%5D%3D=" + testList[0] + "&x%5B%5D%3D=" + testList[1], data.url);
    });
    it("should be correct for list nullable query param", function () {
        let path = "/uuid-list-null";
        var data = jsRoutes.controllers.UUIDController.queryListNullable(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListNullable();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListNullable(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListNullable([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListNullable(testList);
        assert.equal(path + "?x%5B%5D%3F=" + testList[0] + "&x%5B%5D%3F=" + testList[1], data.url);
    });
    it("should be correct for list optional query param", function () {
        let path = "/uuid-list-opt";
        var data = jsRoutes.controllers.UUIDController.queryListOptional(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListOptional();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListOptional(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListOptional([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListOptional(testList);
        assert.equal(path + "?x%5B%5D%3F=" + testList[0] + "&x%5B%5D%3F=" + testList[1], data.url);
    });
    it("should be correct for list optional default query param", function () {
        let path = "/uuid-list-opt-d";
        var data = jsRoutes.controllers.UUIDController.queryListOptionalDefault(null);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListOptionalDefault();
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListOptionalDefault(undefined);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListOptionalDefault([]);
        assert.equal(path, data.url);
        data = jsRoutes.controllers.UUIDController.queryListOptionalDefault(defaultList);
        assert.equal(path + "?x%5B%5D%3F%3D=" + defaultList[0] + "&x%5B%5D%3F%3D=" + defaultList[1], data.url);
        data = jsRoutes.controllers.UUIDController.queryListOptionalDefault(testList);
        assert.equal(path + "?x%5B%5D%3F%3D=" + testList[0] + "&x%5B%5D%3F%3D=" + testList[1], data.url);
    });
});
