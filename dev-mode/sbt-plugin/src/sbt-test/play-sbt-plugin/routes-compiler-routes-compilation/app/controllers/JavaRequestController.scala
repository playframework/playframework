/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import scala.jdk.CollectionConverters._

import jakarta.inject.Inject
import models.UserId
import play.mvc.Http
import play.mvc.Result
import play.mvc.Results

// Java style actions which get the Java request passed by the router, see the `/java-request` routes
class JavaRequestController @Inject() () {
  def index(r: Http.Request): Result = {
    Results.ok(r.uri())
  }
  def post(req: Http.Request): Result = {
    Results.ok(req.uri())
  }
  def withParam(request: Http.Request, param: String): Result = {
    Results.ok(request.uri() + " " + param)
  }
  def user(userId: UserId, req: Http.Request): Result = {
    Results.ok(req.uri() + " " + userId.id)
  }
  def queryUser(req: Http.Request, userId: UserId): Result = {
    Results.ok(req.uri() + " " + userId.id)
  }
  def takeInt(req: Http.Request, i: java.lang.Integer): Result = {
    Results.ok(req.uri() + " " + i)
  }
  def takeBool(b: java.lang.Boolean, req: Http.Request): Result = {
    Results.ok(req.uri() + " " + b)
  }
  def takeBool2(b: java.lang.Boolean, req: Http.Request): Result = {
    Results.ok(req.uri() + " " + b)
  }
  def takeList(req: Http.Request, x: java.util.List[java.lang.Integer]): Result = {
    Results.ok(req.uri() + " " + x.asScala.mkString(","))
  }
  def takeJavaList(x: java.util.List[java.lang.Integer], req: Http.Request): Result = {
    Results.ok(req.uri() + " " + x.asScala.mkString(","))
  }
  def urlcoding(dynamic: String, static: String, req: Http.Request, query: String): Result = {
    Results.ok(req.uri() + " dynamic=" + dynamic + " static=" + static + " query=" + query)
  }
  def route(req: Http.Request, parameter: String): Result = {
    Results.ok(req.uri() + " " + parameter)
  }
  def routetest(req: Http.Request, parameter: String): Result = {
    Results.ok(req.uri() + " " + parameter)
  }
  def routedefault(req: Http.Request, parameter: String): Result = {
    Results.ok(req.uri() + " " + parameter)
  }
  def hello(req: Http.Request): Result = {
    Results.ok(req.uri() + " Hello world!")
  }
  def interpolatorWarning(req: Http.Request, parameter: String): Result = {
    Results.ok(req.uri() + " " + parameter)
  }
}

class JavaRequestInstanceController @Inject() () {
  var invoked = 0

  def index(request: Http.Request): Result = {
    invoked += 1
    Results.ok(request.uri() + " " + invoked)
  }
}
