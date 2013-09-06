package models;

import java.util.*;
import javax.persistence.*;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

import com.avaje.ebean.*;

/**
 * Task entity managed by Ebean
 */
@Entity 
public class Task extends Model {

    private static final long serialVersionUID = 1L;

	@Id
    public Long id;
    
    @Constraints.Required
    public String title;
    
    public boolean done = false;
    
    @Formats.DateTime(pattern="MM/dd/yy")
    public Date dueDate;
    
    @ManyToOne
    public User assignedTo;
    
    public String folder;
    
    @ManyToOne
    public Project project;
    
    // -- Queries
    
    public static Model.Finder<Long,Task> find = new Model.Finder<Long,Task>(Long.class, Task.class);
    
    /**
     * Retrieve todo tasks for the user.
     */
    public static List<Task> findTodoInvolving(String user) {
       return find.fetch("project")
           .where()
                .eq("done", false)
                .eq("project.members.email", user)
           .findList();
    }
    
    /**
     * Find tasks related to a project
     */
    public static List<Task> findByProject(Long project) {
        return Task.find.where()
            .eq("project.id", project)
            .findList();
    }
    
    /**
     * Delete all tasks in a folder
     */
    public static void deleteInFolder(Long project, String folder) {
        Ebean.createSqlUpdate(
            "delete from task where folder = :folder and project_id = :project"
        ).setParameter("folder", folder)
         .setParameter("project", project)
         .execute();
    }
    
    /**
     * Rename a folder
     */
    public static String renameFolder(Long project, String folder, String newName) {
        Ebean.createSqlUpdate(
            "update task set folder = :newName where folder = :folder and project_id = :project"
        ).setParameter("folder", folder)
            .setParameter("newName", newName)
            .setParameter("project", project)
            .execute();
        return newName;
    }
    
    /**
     * Create a task
     */
    public static Task create(Task task, Long project, String folder) {
        task.project = Project.find.ref(project);
        task.folder = folder;
        task.save();
        return task;
    }
    
    /**
     * Mark a task as done or not
     */
    public static void markAsDone(Long taskId, Boolean done) {
        Task task = Task.find.ref(taskId);
        task.done = done;
        task.update();
    }
    
    /**
     * Check if a user is the owner of this task
     */
    public static boolean isOwner(Long task, String user) {
        return find.where()
            .eq("project.members.email", user)
            .eq("id", task)
            .findRowCount() > 0;
    }

    // --

    public String toString() {
        return "Task(" + id + ") in project " + project;
    }

}

