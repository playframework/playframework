package controllers;

import java.util.concurrent.Callable;

import models.User;
import com.fasterxml.jackson.databind.JsonNode;
import play.*;
import play.libs.Akka;
import play.libs.F.Function;
import play.mvc.*;

import views.html.*;

public class Application extends Controller {

    public static Result index(String name) {
        Http.Context.current().args.put("name",name);
        String n = (String)Http.Context.current().args.get("name");
        return ok(index.render(n));
    }

    public static Result key() {
        return ok("Key=" + Play.application().configuration().getString("key"));
    }

    public static Result getIdenticalJson() {
        JsonNode node = request().body().asJson();
        return ok(node);
    }

    public static Result asyncResult() {
        return async(Akka.future(new Callable<String>() {
            @Override
            public String call() {
                return "success";
            }
        }).map(new Function<String, Result>() {
            @Override
            public Result apply(String a) {
                response().setHeader("header_test", "header_val");
                response().setCookie("cookie_test", "cookie_val");
                session("session_test", "session_val");
                flash("flash_test", "flash_val");
                return ok(a);
            };
        }));
    }

    public static Result setLang(String code) {
        changeLang(code);
        return ok(lang().code());
    }

    public static Result unsetLang() {
        clearLang();
        return ok(lang().code());
    }

    public static Result hello() {
        return ok(play.i18n.Messages.get("hello"));
    }

    public static Result paged(Pager pager) {
        return ok(pager.toString());
    }

    public static Result user(User user) {
        return ok(user.email);
    }

    public static Result thread() {
        return ok(Thread.currentThread().getName());
    }
}
