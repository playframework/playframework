package play.libs;

public class JavaPOJO {

    private String foo;
    private String bar;

    public JavaPOJO() {
    }

    public JavaPOJO(String foo, String bar) {
        this.foo = foo;
        this.bar = bar;
    }

    public String getFoo() {
        return foo;
    }

    public void setFoo(String foo) {
        this.foo = foo;
    }

    public String getBar() {
        return bar;
    }

    public void setBar(String bar) {
        this.bar = bar;
    }
}
