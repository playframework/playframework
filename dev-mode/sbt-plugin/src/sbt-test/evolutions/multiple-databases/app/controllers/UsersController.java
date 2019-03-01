/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers;

import models.*;

import play.db.*;
import play.mvc.*;

import java.sql.*;
import java.util.*;
import javax.inject.*;

public class UsersController extends Controller {

    private final Database db;

    @Inject
    public UsersController(@NamedDatabase("users") Database db) {
        this.db = db;
    }

    public Result list() {
        List<User> users = db.withConnection(connection -> {
            List<User> result = new ArrayList<>();
            PreparedStatement statement = connection.prepareStatement("select id, username from users");
            ResultSet rs = statement.executeQuery();

            while(rs.next()) {
                Long id = rs.getLong("id");
                String username = rs.getString("username");

                result.add(new User(id, username));
            }

            return result;
        });
        return ok(views.html.users.render(users));
    }

}
