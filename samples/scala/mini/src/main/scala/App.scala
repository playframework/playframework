package com.example 

import play.api._
import play.api.mvc._
import play.api.mvc.Results._

/**
 * this application is registered via Global
 */
class App extends mini.Application { 
  def dispatcher = Map("/hello" -> Action{ Ok(<h1>It works!</h1>).as("text/html") })
}


