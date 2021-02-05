package outside;

public class Bar {
    public static void fail() {
        throw new RuntimeException("Exception thrown in sub-project-outside");
    }
}
