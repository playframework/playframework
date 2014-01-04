/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http.routing.controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class Clients extends Controller {

    //#clients-show-action
    public static Result show(Long id) {
        Client client = Client.findById(id);
        return ok(views.html.Client.show(client));
    }
    //#clients-show-action

    public static Result list() {
        return ok("all clients");
    }

    static class Client {
        Client(Long id) {
            this.id = id;
        }
        Long id;
        static Client findById(Long id) {
            return new Client(id);
        }
        String show(Client client) {
            return "showing client " + client.id;
        }
    }
    static Views views = new Views();
    static class Views {
        Html html = new Html();
    }
    static class Html {
        Client Client = new Client(0l);
    }

}
