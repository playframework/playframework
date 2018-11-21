(DISCLAIMER: this file is located under `shutdown/happy-path` to avoid https://github.com/sbt/sbt/issues/4457)
# Shutdown test suite

This collection of scripted tests helps ensuring the correct resource de-alloc 
in as many scenarios as possible.

Tests are grouped in `scripted` suites to reduce the maintainability costs and time 
to execute at the cost of loosing some visibility when a test fails. 

Here's a list of `scripted` suites and what are the tests they exercise:

 * `happy-path`:
    * `6-`: Mode.Dev + file change causes dev mode reload
    * `5-`: Mode.Dev + user stops dev mode
    * `3-`: Mode.Test + non-forked tests
    * `1-`: Mode.Prod finished on `SIGTERM`
 * `downing`:
    * `7-`: Mode.Dev + on `Down`ing, stop dev mode 
    * `4-`: Mode.Test + forked tests
    * `2-`: Mode.Prod finished on `Down`ing

## Using default settings

### Mode.Prod

1- Process finishes on `SIGTERM`

2- Process finishes on programmatic event (e.g. cluster `Down`ing event) 

### Mode.Test

3- non-forked tests execute completely, run coordinated shutdown but don't exit the JVM

4- forked tests execute completely, run coordinated shutdown and exit the JVM

### Mode.Dev

5- on user interaction: stop dev mode, run coordinated shutdown but don't exit the JVM

6- on file change: run coordinated shutdown but don't exit the JVM

7- on programmatic event (Downing): stop dev mode, run coordinated shutdown but don't exit the JVM
 
## PID file 

Running Play in Mode.Prod requires producing a `pidfile` that must be destroyed when the 
process completes.

That file must only exist during thhe life-span of a PROD process (never TEST nor DEV).

## Using custom settings
 
a- Using `akka.coordinated-shutdown.exit-jvm` is forbidden and Mode.Prod doesn't start 

b- Using `akka.coordinated-shutdown.reason-overrides....exit-jvm` for a custom reason is honored 

c- (TODO) Using a custom `exit-code` is honored

d- (TODO) Using a custom `exit-code` for a custom reason is honored