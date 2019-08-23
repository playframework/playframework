/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
var assert = require("assert");
var jsRoutes = require("./jsRoutes");
var jsRoutesBadHost = require("./jsRoutesBadHost");

describe("The JavaScript router", function() {
    it("should generate a url", function() {
        var data = jsRoutes.router.Application.index();
        assert.equal("/", data.url);
    });
    it("should provide the GET method", function() {
        var data = jsRoutes.router.Application.index();
        assert.equal("GET", data.method);
    });
    it("should provide the POST method", function() {
        var data = jsRoutes.router.Application.post();
        assert.equal("POST", data.method);
    });
    it("should add parameters to the path", function() {
        var data = jsRoutes.router.Application.withParam("foo");
        assert.equal("/with/foo", data.url);
    });
    it("should add parameters to the query string", function() {
        var data = jsRoutes.router.Application.takeBool(true);
        assert.equal("/take-bool?b=true", data.url);
    });
    it("should add complex named parameters to the query string", function() {
        var data = jsRoutes.router.Application.takeListTickedParam([1,2,3]);
        var pname = encodeURI('b[]');
        qs = [1,2,3].map(function(i){return pname + '=' + i}).join('&');
        assert.equal("/take-list-tick-param?" + qs, data.url);
    });
    it("should avoid name collisions on query string with complex names", function() {
        var data = jsRoutes.router.Application.takeTickedParams([1,2,3], "c");
        var pname1 = encodeURI('b[]');
        var pname2 = encodeURI('b%%')
        qs = [1,2,3].map(function(i){return pname1 + '=' + i}).concat(pname2 + '=c').join('&');
        assert.equal("/take-ticked-params?" + qs, data.url);
    });
    it("should properly escape the host", function() {
        var data = jsRoutesBadHost.router.Application.index();
        assert(data.absoluteURL().indexOf("'}}};alert(1);a={a:{a:{a:'") >= 0)
    });
});
