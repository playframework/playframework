//#dependency-injection
//###replace: package controllers
package javaguide.configuration;

import com.typesafe.config.Config;
import play.mvc.Controller;

import javax.inject.Inject;

public class MyController extends Controller {

    private final Config config;

    @Inject
    public MyController(Config config) {
        this.config = config;
    }
}
//#dependency-injection