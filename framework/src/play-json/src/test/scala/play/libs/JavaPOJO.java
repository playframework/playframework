package play.libs;

import java.time.Instant;
import java.util.Optional;

public class JavaPOJO {

    private String foo;
    private String bar;
    private Instant instant;
    private Optional<Long> optLong;

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

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public Optional<Long> getOptLong() {
        return optLong;
    }

    public void setOptLong(Optional<Long> optLong) {
        this.optLong = optLong;
    }
}
