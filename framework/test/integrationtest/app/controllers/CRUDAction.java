package controllers;

import play.*;
import play.mvc.*;

import views.html.*;

public class CRUDAction extends ParentAction {
  private static CRUDAction instance = new CRUDAction();
  public static CRUDAction methods() { return instance; }
}
