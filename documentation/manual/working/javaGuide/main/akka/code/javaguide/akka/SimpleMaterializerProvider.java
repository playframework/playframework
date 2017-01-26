/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.akka;
//#overwritten-materializer
import akka.actor.ActorSystem;
import akka.stream.Supervision;
import com.google.inject.Inject;
import play.api.libs.concurrent.DefaultAkkaMaterializerProvider;

import javax.inject.Singleton;

@Singleton
public class SimpleMaterializerProvider extends DefaultAkkaMaterializerProvider {

    private int errorCount = 0;

    @Inject
    public SimpleMaterializerProvider(ActorSystem actorSystem) {
        super(actorSystem);
    }

    @Override
    public java.util.function.Function<Throwable, Supervision.Directive> supervisionDecider() {
        return param -> {
            System.out.println("Error" + this.toString());
            errorCount = errorCount + 1;
            System.out.println("count: " + errorCount);
            return Supervision.stop();
        };
    }

    public int getErrorCount() {
        System.out.println("COUNT GET: " + errorCount + this.toString());
        return errorCount;
    }

}
//#overwritten-materializer