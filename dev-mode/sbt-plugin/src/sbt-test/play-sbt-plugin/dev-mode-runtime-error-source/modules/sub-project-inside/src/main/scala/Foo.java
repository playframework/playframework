package inside;

public class Foo {
    public static void fail() {
        throw new RuntimeException("Exception thrown in sub-project-inside");
    }
}
