# Configuring the internal Akka system

Play 2.0 uses an internal Akka Actor system to handle request processing. You can configure it in your application `application.conf` configuration file.

## Action invoker actors

The action invoker Actors are used to execute the `Action` code. To be able to execute several Actions concurrently we are using several of these Actors managed by a Round Robin router. These actors are stateless.

These action invoker Actors are also used to retrieve the **body parser** needed to parse the request body. Because this part waits for a reply (the `BodyParser` object to use), it will fail after a configurable timeout.

Action invoker actors are run by the `actions-dispatcher` dispatcher.

## Promise invoker actors

The promise invoker Actors are used to execute all asynchronous callback needed by `Promise`. Several Actors must be available to execute several Promise callbacks concurrently. These actors are stateless.

Promise invoker actors are run by the `promises-dispatcher` dispatcher.

## WebSockets agent actors

Each WebSocket connection state is managed by an Agent actor. A new actor is created for each WebSocket, and is killed when the socket is closed. These actors are statefull.

WebSockets agent actors are run by the `websockets-dispatcher` dispatcher.

## Default configuration

Here is the reference configuration used by Play 2.0 if you don't override it. Adapt it according your application needs.

```
play {
    
    akka {
        event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
        loglevel = WARNING
        
        actor {
            
            deployment {

                /actions {
                    router = round-robin
                    nr-of-instances = 24
                }

                /promises {
                    router = round-robin
                    nr-of-instances = 24
                }

            }
            
            retrieveBodyParserTimeout = 1 second
            
            actions-dispatcher = {
                fork-join-executor {
                    parallelism-factor = 1.0
                    parallelism-max = 24
                }
            }

            promises-dispatcher = {
                fork-join-executor {
                    parallelism-factor = 1.0
                    parallelism-max = 24
                }
            }

            websockets-dispatcher = {
                fork-join-executor {
                    parallelism-factor = 1.0
                    parallelism-max = 24
                }
            }

            default-dispatcher = {
                fork-join-executor {
                    parallelism-factor = 1.0
                    parallelism-max = 24
                }
            }
            
        }
        
    }
    
}
```