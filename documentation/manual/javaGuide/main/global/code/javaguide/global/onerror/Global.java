package javaguide.global.onerror;

import javaguide.global.views;
//#global
import play.*;
import play.mvc.*;
import play.mvc.Http.*;
import play.libs.F.*;

import static play.mvc.Results.*;

public class Global extends GlobalSettings {

    public Promise<SimpleResult> onError(RequestHeader request, Throwable t) {
        return Promise.<SimpleResult>pure(internalServerError(
            views.html.errorPage.render(t)
        ));
    }

}
//#global
