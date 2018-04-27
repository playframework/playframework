package javaguide.hello;

import play.mvc.*;

public class HelloController extends Controller {

    //#hello-world-index-action
    public Result index() {
        return ok(views.html.index.render());
    }
    //#hello-world-index-action

    //#hello-world-hello-action
    public Result hello() {
        return ok(views.html.hello.render());
    }
    //#hello-world-hello-action

    /*
    //#hello-world-hello-error-action
    public Result hello(String name) {
        return ok(views.html.hello.render());
    }
    //#hello-world-hello-error-action
     */

    /*
    //#hello-world-hello-correct-action
    public Result hello(String name) {
        return ok(views.html.hello.render(name));
    }
    //#hello-world-hello-correct-action
     */
}