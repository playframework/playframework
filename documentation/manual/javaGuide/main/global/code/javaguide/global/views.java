package javaguide.global;

// Hack to allow example Globals to refer to templates in the views.html package without them actually being in that
// package.
public class views {
    public static HtmlField html = new HtmlField();

    public static class HtmlField {
        public static javaguide.global.html.errorPage$ errorPage = javaguide.global.html.errorPage$.MODULE$;
        public static javaguide.global.html.notFoundPage$ notFoundPage = javaguide.global.html.notFoundPage$.MODULE$;
    }
}
