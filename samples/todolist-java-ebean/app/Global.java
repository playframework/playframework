import models.Task;
import org.joda.time.DateTime;
import play.Application;
import play.GlobalSettings;
import play.data.format.Formats;
import play.mvc.Result;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Executes some database setup when the application starts.
 * The class name must be Global.
 */
public final class Global extends GlobalSettings {
    /**
     * Executed before any plugin is loaded. This is a good place to perform system operation.
     *
     * @param app is the current application
     */
    @Override
    public void beforeStart(Application app) {
        System.out.println("beforeStart called");
    }

    /**
     * Executed once all plugins have been started : the EBean and the Database should be up and ready
     * to accept some operation.
     *
     * @param app is the current application
     */
    @Override
    public void onStart(Application app) {
        System.out.println("onStart...");

        // Create some test entries
        if(Task.count()==0) {
            Task task1=new Task();
            task1.name="Subscribe to Play! Developer's list";
            task1.dueDate=new DateTime().plusDays(3).toDate();
            task1.save();

            Task task2=new Task();
            task2.name="Send a giftcard to Emma";
            task2.dueDate=new DateTime().plusMonths(1).toDate();
            task2.save();

            Task task3=new Task();
            task3.name="Buy a Devoxx ticket";
            task3.dueDate=new DateTime().minusDays(5).toDate();
            task3.done=Boolean.TRUE;
            task3.save();
        }
    }

    /**
     * Executed before shutting down the application
     *
     * @param app is the current application
     */
    @Override
    public void onStop(Application app) {
        System.out.println("onStop...");
    }

    /**
     * Triggered by the framework upon error. Here you can return a custom result for a specific throwable.
     * If you return null, Play will display its custom error page in DEV mode or a server error in Prod mode.
     *
     * @param t is any throwable
     * @return my custom result or null.
     */
    @Override
    public Result onError(Throwable t) {
        System.out.println("onError with " + t);
        return null; // if you return null, Play will delegate to the default error handler.
    }

    /**
     * Triggered when an action was not found.
     *
     * @param uri is the request
     * @return either a custom Result or null.
     */
    @Override
    public Result onActionNotFound(String uri) {
        if (uri != null) {
            return new Result.NotFound("Resource not found: " + uri);
        } else {
            return super.onActionNotFound(uri);
        }
    }
}
