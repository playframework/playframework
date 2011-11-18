package com.example;

import play.*;
import play.mvc.*;
import play.mini.*;

/**
 * this app is configured through Global.scala
 */
public class App extends Controller {

   @URL("/hello")
   public static Result index() {
     return ok("It works!");
   }
}
