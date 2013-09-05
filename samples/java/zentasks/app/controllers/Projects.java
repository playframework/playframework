package controllers;

import play.mvc.*;
import static play.data.Form.*;

import java.util.*;

import models.*;

import views.html.*;
import views.html.projects.*;

/**
 * Manage projects related operations.
 */
@Security.Authenticated(Secured.class)
public class Projects extends Controller {
  
    /**
     * Display the dashboard.
     */
    public static Result index() {
        return ok(
            dashboard.render(
                Project.findInvolving(request().username()),
                Task.findTodoInvolving(request().username()),
                User.find.byId(request().username())
            )
        );
    }

    // -- Projects

    /**
     * Add a project.
     */
    public static Result add() {
        Project newProject = Project.create(
            "New project", 
            form().bindFromRequest().get("group"),
            request().username()
        );
        return ok(item.render(newProject));
    }
    
    /**
     * Rename a project.
     */
    public static Result rename(Long project) {
        if(Secured.isMemberOf(project)) {
            return ok(
                Project.rename(
                    project, 
                    form().bindFromRequest().get("name")
                )
            );
        } else {
            return forbidden();
        }
    }
    
    /**
     * Delete a project.
     */
    public static Result delete(Long project) {
        if(Secured.isMemberOf(project)) {
            Project.find.ref(project).delete();
            return ok();
        } else {
            return forbidden();
        }
    }

    // -- Project groups

    /**
     * Add a new project group.
     */
    public static Result addGroup() {
        return ok(
            group.render("New group", new ArrayList<Project>())
        );
    }
  
    /**
     * Delete a project group.
     */
    public static Result deleteGroup(String group) {
        Project.deleteInFolder(group);
        return ok();
    }
  
    /**
     * Rename a project group.
     */
    public static Result renameGroup(String group) {
        return ok(
            Project.renameFolder(group, form().bindFromRequest().get("name"))
        );
    }
  
    // -- Members
  
    /**
     * Add a project member.
     */
    public static Result addUser(Long project) {
        if(Secured.isMemberOf(project)) {
            Project.addMember(
                project,
                form().bindFromRequest().get("user")
            );
            return ok();
        } else {
            return forbidden();
        }
    }
  
    /**
     * Remove a project member.
     */
    public static Result removeUser(Long project) {
        if(Secured.isMemberOf(project)) {
            Project.removeMember(
                project,
                form().bindFromRequest().get("user")
            );
            return ok();
        } else {
            return forbidden();
        }
    }
  
}

