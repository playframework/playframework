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
            errorCount = errorCount + 1;
            return Supervision.stop();
        };
    }

    public int getErrorCount() {
        return errorCount;
    }

}
//#overwritten-materializer