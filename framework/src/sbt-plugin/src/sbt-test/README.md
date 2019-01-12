# Play Plugin Tests

This is a collection of sbt scripted tests for Play's sbt-plugin. They are organized in distinct groups to test specific features provided by Play plugin.

## Evolutions test suite

Verifies that Play evolutions work as expected for the following scenarios:

### `DEV` mode

1. Ask to apply evolutions when there are changes to apply and `autoApply=false`:
    1. Successfully applies evolutions when requested
    2. Detects new evolution as ask to apply
2. Applies evolutions without asking when `autoApply=true`
    1. Successfully applies evolutions without user intervention
    2. Detects new evolution applies it automatically
3. Shows an error when there is an error in one of the evolutions
    1. Accepts the user request to consider the error manually fixed
4. Handle evolutions for multiple databases
    1. When both are configured to `autoApply=false`
    2. When one is configured to `autoApply=false` and the other one is `autoApply=true`
    3. Should ask to apply when there is a new evolution to the db with `autoApply=false`
    4. Should apply a new evolution automatically when for the db with `autoApply=true`
    
### `PROD` mode

1. Should start successfully when there are evolutions and `autoApply=true`
2. Should fail to start when there are evolutions and `autoApply=false`

## Shutdown test suite

This collection of scripted tests helps ensuring the correct resource de-alloc 
in as many scenarios as possible.

Tests are grouped in `scripted` suites to reduce the maintainability costs and time 
to execute at the cost of loosing some visibility when a test fails. 

Here's a list of `scripted` suites and what are the tests they exercise:

 * `happy-path`:
    * `6-`: Mode.Dev + file change causes dev mode reload
    * `5-`: Mode.Dev + Ctrl-D stops dev mode
    * `3-`: Mode.Test + non-forked tests
    * `1-`: Mode.Prod finished on `SIGTERM`
 * `downing`:
    * `7-`: Mode.Dev + on `Down`ing, stop dev mode 
    * `4-`: Mode.Test + forked tests
    * `2-`: Mode.Prod finished on `Down`ing

### General requirements

#### i. PID file 

Only when running Play in Mode.Prod requires producing a `pidfile` that must be deleted when the 
process completes.

That file must only exist during the life-span of a PROD process (never TEST nor DEV).


### Using default settings

There's a first batch of use cases to be tested with default settings.

#### Mode.Prod

1- Process finishes on `SIGTERM`

2- Process finishes on programmatic event (e.g. cluster `Down`ing event) 

#### Mode.Test

3- non-forked tests execute completely, run coordinated shutdown but don't exit the JVM

4- forked tests execute completely, run coordinated shutdown and exit the JVM

#### Mode.Dev

5- on user interaction (Ctrl-D): stop dev mode, run coordinated shutdown but don't exit the JVM

6- on file change: only the Application should die, run coordinated shutdown but don't exit the JVM

7- on programmatic event (e.g. `Down`ing): only the Application should die, run coordinated shutdown but don't exit the JVM

### Using custom settings (WIP) 

There's a collection of settings with a certain impact on shutdown that deserve careful testing. Below is a 
list of those settings. Using each of these settings may require on or many tests from the `Using default settings` list above.
 
a- Using `akka.coordinated-shutdown.exit-jvm` is forbidden and Mode.Prod doesn't start 

b- Using `akka.coordinated-shutdown.reason-overrides....exit-jvm` for a custom reason is honored 

c- (TODO) Using a custom `exit-code` is honored

d- (TODO) Using a custom `exit-code` for a custom reason is honored