package controllers;

import models.Computer;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.createForm;
import views.html.editForm;
import views.html.list;
import controllers.Interceptor.CommonHeader;
import controllers.Interceptor.ComputerContext;

/**
 * Manage a database of computers
 */
@CommonHeader
public class Application extends Controller {

    /**
     * Handle default path requests, redirect to computers list
     */
    public static Result index() {
        return list(0, "name", "asc", "");
    }

    /**
     * Display the paginated list of computers.
     *
     * @param page Current page number (starts from 0)
     * @param sortBy Column to be sorted
     * @param order Sort order (either asc or desc)
     * @param filter Filter applied on computer names
     */
    public static Result list(int page, String sortBy, String order, String filter) {
        return ok(
            list.render(
                Computer.page(page, 10, sortBy, order, filter),
                sortBy, order, filter, numComputers()
            )
        );
    }
    
    /**
     * Display the 'edit form' of a existing Computer.
     *
     * @param id Id of the computer to edit
     */
    public static Result edit(Long id) {
        Form<Computer> computerForm = form(Computer.class).fill(
            Computer.find.byId(id)
        );
        return ok(
            editForm.render(id, computerForm, numComputers())
        );
    }
    
    /**
     * Handle the 'edit form' submission 
     *
     * @param id Id of the computer to edit
     */
    public static Result update(Long id) {
        Form<Computer> computerForm = form(Computer.class).bindFromRequest();
        if(computerForm.hasErrors()) {
            return badRequest(editForm.render(id, computerForm, numComputers()));
        }
        computerForm.get().update(id);
        flash("success", "Computer " + computerForm.get().name + " has been updated");
        return index();
    }
    
    /**
     * Display the 'new computer form'.
     */
    public static Result create() {
        Form<Computer> computerForm = form(Computer.class);
        return ok(
            createForm.render(computerForm, numComputers())
        );
    }
    
    /**
     * Handle the 'new computer form' submission 
     */
    public static Result save() {
        Form<Computer> computerForm = form(Computer.class).bindFromRequest();
        if(computerForm.hasErrors()) {
            return badRequest(createForm.render(computerForm, numComputers()));
        }
        computerForm.get().save();
        flash("success", "Computer " + computerForm.get().name + " has been created");
        return index();
    }
    
    /**
     * Handle computer deletion
     */
    public static Result delete(Long id) {
        Computer.find.ref(id).delete();
        flash("success", "Computer has been deleted");
        return index();
    }
    
    public static int numComputers() {
      return ((ComputerContext) ctx()).numComputers();
    }
    
}
            
