/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import models.*;

import play.db.*;
import play.mvc.*;

import java.sql.*;
import java.util.*;
import jakarta.inject.*;

public class GroupsController extends Controller {

    private final Database db;
    private final AssetsFinder assetsFinder;

    @Inject
    public GroupsController(@NamedDatabase("groups") Database db, AssetsFinder assetsFinder) {
        this.db = db;
        this.assetsFinder = assetsFinder;
    }

    public Result list() {
        List<Group> groups = db.withConnection(connection -> {
            List<Group> result = new ArrayList<>();
            PreparedStatement statement = connection.prepareStatement("select id, name from groups");
            ResultSet rs = statement.executeQuery();

            while(rs.next()) {
                Long id = rs.getLong("id");
                String name = rs.getString("name");

                result.add(new Group(id, name));
            }

            return result;
        });
        return ok(views.html.groups.render(groups, assetsFinder));
    }

}
