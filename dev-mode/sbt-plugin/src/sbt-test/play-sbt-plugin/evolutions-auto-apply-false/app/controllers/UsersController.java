/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
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
    public UsersController(Database db) {
        this.db = db;

        insertRow(db, "PlayerFromControllerInit");
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
        return ok(views.html.index.render(users));
    }

    public static void insertRow(Database db, String text) {
        db.withConnection(connection -> {
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO users (username) VALUES (?)");
            stmt.setString(1, text);
            stmt.execute();
        });
    }

}
