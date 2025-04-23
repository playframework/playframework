/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.http.routing.controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class Clients extends Controller {

  // #clients-show-action
  public Result show(Long id) {
    Client client = clientService.findById(id);
    return ok(views.html.Client.show(client));
  }

  // #clients-show-action

  public Result list() {
    return ok("all clients");
  }

  static class clientService {
    static Client findById(Long id) {
      return new Client(id);
    }
  }

  static class Client {
    Client(Long id) {
      this.id = id;
    }

    Long id;

    String show(Client client) {
      return "showing client " + client.id;
    }
  }

  static class views {
    static class html {
      static Client Client = new Client(0l);
    }
  }
}
